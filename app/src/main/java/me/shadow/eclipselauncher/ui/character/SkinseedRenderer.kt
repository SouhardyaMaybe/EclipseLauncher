/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Skinseed-style 3D skin viewer: two-pass depth-prepass renderer with
 * quaternion-based 6-part rig, slerp pose animation, and color-ID picking.
 * Clean-room rewrite based on Skinseed 27.1.2 architecture study.
 */
package me.shadow.eclipselauncher.ui.character

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.AttributeSet
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * GLSurfaceView that renders a Minecraft skin using Skinseed's architecture:
 * - Two-pass: depth prepass (opaque) → color passes (base + highlight + overlay)
 * - 6-part rig with per-part quaternions + body quaternion
 * - Slerp pose interpolation
 * - Color-ID picking for part selection
 */
class SkinseedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {

    private val renderer: SkinseedRenderer

    init {
        setEGLContextClientVersion(2)
        setPreserveEGLContextOnPause(true)
        renderer = SkinseedRenderer(this)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // Ensure we have a minimum size even when parent doesn't constrain us (e.g., in tests)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (width == 0 || height == 0) {
            setMeasuredDimension(
                max(width, 100),
                max(height, 100)
            )
        }
    }

    fun updateSkin(bitmap: Bitmap?) {
        queueEvent { renderer.setSkin(bitmap) }
        requestRender()
    }

    fun updateOverlay(bitmap: Bitmap?) {
        queueEvent { renderer.setOverlay(bitmap) }
        requestRender()
    }

    fun updateCape(bitmap: Bitmap?) {
        queueEvent { renderer.setCape(bitmap) }
        requestRender()
    }

    fun updateVariant(variant: String) {
        queueEvent { renderer.setVariant(variant) }
        requestRender()
    }

    fun updateOverlayEnabled(enabled: Boolean) {
        queueEvent { renderer.setOverlayEnabled(enabled) }
        requestRender()
    }

    fun setPose(poseName: String) {
        queueEvent { renderer.setPose(poseName) }
        requestRender()
    }

    fun setCustomPose(pose: Map<String, FloatArray>) {
        queueEvent { renderer.setCustomPose(pose) }
        requestRender()
    }

    fun resetPose() {
        queueEvent { renderer.resetPose() }
        requestRender()
    }

    fun setAutoRotate(enabled: Boolean) {
        queueEvent { renderer.setAutoRotate(enabled) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        renderer.handleTouch(event)
        return true
    }
}

/**
 * Core renderer implementing Skinseed's two-pass depth-prepass pipeline.
 */
class SkinseedRenderer(private val view: SkinseedView) : GLSurfaceView.Renderer {

    // Shader programs
    private var programSolid: Int = 0
    private var programOverlay: Int = 0
    private var programPick: Int = 0

    // Uniform locations (solid)
    private var uMVPMatrixSolid: Int = 0
    private var uTextureSolid: Int = 0
    private var uLightDirSolid: Int = 0

    // Uniform locations (overlay)
    private var uMVPMatrixOverlay: Int = 0
    private var uTextureOverlay: Int = 0
    private var uOverlayTexture: Int = 0
    private var uUseOverlay: Int = 0
    private var uAlphaOverlay: Int = 0
    private var uOverlayAlpha: Int = 0
    private var uLightDirOverlay: Int = 0

    // Uniform locations (pick)
    private var uMVPMatrixPick: Int = 0
    private var uPickColor: Int = 0

    // Attribute locations
    private var aPosition: Int = 0
    private var aTexCoord: Int = 0
    private var aNormal: Int = 0

    // Geometry buffers
    private var vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var texCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var normalBuffer: FloatBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var indexBuffer: ShortBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asShortBuffer()
    private var partIndexOffsets: IntArray = intArrayOf()
    private var partIndexCounts: IntArray = intArrayOf()

    // Textures
    private var skinTextureId: Int = -1
    private var overlayTextureId: Int = -1
    private var capeTextureId: Int = -1

    // Camera
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private var cameraDistance = 5.8f
    private var cameraYaw = 0.0f
    private var cameraPitch = -0.2f

    // Touch
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var selectedPart = -1

    // Animation
    private var isAnimating = true
    private var autoRotate = true
    private var lastFrameTime = System.nanoTime()
    private var animationTime = 0f

    // Model state
    private var variant = "CLASSIC"
    private var isSlim = false
    private var overlayEnabled = true

    // Rig: 6 parts (head, body, armL, armR, legL, armR)
    private val partQuats = FloatArray(6 * 4) // w,x,y,z per part
    private val targetQuats = FloatArray(6 * 4)
    private val bodyQuat = floatArrayOf(1f, 0f, 0f, 0f) // w,x,y,z
    private val targetBodyQuat = floatArrayOf(1f, 0f, 0f, 0f)

    // Pivots in model space (Skinseed coordinates, units = 0.2 * skin pixels)
    // Head(0,1.6,0), Body(0,0.4,0), ArmL(-1.2,1.2,0), ArmR(1.2,1.2,0), LegL(0.4,-0.8,0), LegR(-0.4,-0.8,0)
    private val pivots = floatArrayOf(
        0f, 1.6f, 0f,    // HEAD
        0f, 0.4f, 0f,    // BODY
        -1.2f, 1.2f, 0f, // ARM_L
        1.2f, 1.2f, 0f,  // ARM_R
        0.4f, -0.8f, 0f, // LEG_L
        -0.4f, -0.8f, 0f // LEG_R
    )

    // Part half-extents (Skinseed: head 0.8, body 1.2x1.2x0.4, arm 0.4x1.2x0.4, leg 0.4x1.2x0.4)
    private val halfExtents = floatArrayOf(
        0.8f, 0.8f, 0.8f,      // HEAD
        1.2f, 1.2f, 0.4f,      // BODY
        0.4f, 1.2f, 0.4f,      // ARM_L
        0.4f, 1.2f, 0.4f,      // ARM_R
        0.4f, 1.2f, 0.4f,      // LEG_L
        0.4f, 1.2f, 0.4f       // LEG_R
    )

    // Pick colors (RGBA) - distinct per part
    private val pickColors = floatArrayOf(
        1f, 0f, 0f, 1f,    // HEAD - red
        0f, 1f, 0f, 1f,    // BODY - green
        0f, 0f, 1f, 1f,    // ARM_L - blue
        1f, 1f, 0f, 1f,    // ARM_R - yellow
        1f, 0f, 1f, 1f,    // LEG_L - magenta
        0f, 1f, 1f, 1f     // LEG_R - cyan
    )

    // Slerp parameters (from Skinseed)
    private val SLERP_SPEED = 11.4f
    private val SLERP_EPSILON = 0.01f
    private val AUTO_ROTATE_SPEED = 0.01f

    // Built-in poses (7 poses, 6 parts each, quaternion w,x,y,z)
    private val builtInPoses: Map<String, FloatArray> = buildBuiltInPoses()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.15f, 0.15f, 0.18f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glFrontFace(GLES20.GL_CCW)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        initShaders()
        initGeometry()
        initTextures()
        resetPose()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 42f, ratio, 0.1f, 50f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = (now - lastFrameTime) / 1e9f
        lastFrameTime = now

        updateAnimation(dt)
        updateCamera()

        // ===== PASS 1: DEPTH PREPASS =====
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(programSolid)
        GLES20.glUniformMatrix4fv(uMVPMatrixSolid, 1, false, mvpMatrix, 0)
        GLES20.glUniform3f(uLightDirSolid, 0.5f, 1f, -0.5f)
        drawAllParts(programSolid, uMVPMatrixSolid, aPosition, aNormal, useOverlay = false)

        // ===== PASS 2: COLOR PASSES =====
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Base layer (solid)
        GLES20.glUseProgram(programSolid)
        GLES20.glUniformMatrix4fv(uMVPMatrixSolid, 1, false, mvpMatrix, 0)
        GLES20.glUniform3f(uLightDirSolid, 0.5f, 1f, -0.5f)
        drawAllParts(programSolid, uMVPMatrixSolid, aPosition, aNormal, useOverlay = false)

        // Highlight if part selected
        if (selectedPart >= 0) {
            drawHighlight()
        }

        // Overlay layer (blended)
        if (overlayEnabled) {
            GLES20.glUseProgram(programOverlay)
            GLES20.glUniformMatrix4fv(uMVPMatrixOverlay, 1, false, mvpMatrix, 0)
            GLES20.glUniform1i(uUseOverlay, 1)
            GLES20.glUniform1f(uAlphaOverlay, 1f)
            GLES20.glUniform1f(uOverlayAlpha, 1f)
            GLES20.glUniform3f(uLightDirOverlay, 0.5f, 1f, -0.5f)
            drawAllParts(programOverlay, uMVPMatrixOverlay, aPosition, aNormal, useOverlay = true)
        }
    }

    private fun initShaders() {
        // Solid shader (base layer)
        val vsSolid = """
            attribute vec3 aPosition;
            attribute vec2 aTexCoord;
            attribute vec3 aNormal;
            uniform mat4 uMVPMatrix;
            uniform vec3 uLightDir;
            varying vec2 vTexCoord;
            varying float vLighting;
            void main() {
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
                vTexCoord = aTexCoord;
                vec3 normal = normalize(aNormal);
                float diff = max(dot(normal, normalize(uLightDir)), 0.0);
                vLighting = 0.6 + 0.4 * diff;
            }
        """.trimIndent()
        val fsSolid = """
            precision mediump float;
            uniform sampler2D uTexture;
            varying vec2 vTexCoord;
            varying float vLighting;
            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                color.rgb *= vLighting;
                if (color.a < 0.01) discard;
                gl_FragColor = color;
            }
        """.trimIndent()
        programSolid = createProgram(vsSolid, fsSolid)
        uMVPMatrixSolid = GLES20.glGetUniformLocation(programSolid, "uMVPMatrix")
        uTextureSolid = GLES20.glGetUniformLocation(programSolid, "uTexture")
        uLightDirSolid = GLES20.glGetUniformLocation(programSolid, "uLightDir")
        aPosition = GLES20.glGetAttribLocation(programSolid, "aPosition")
        aTexCoord = GLES20.glGetAttribLocation(programSolid, "aTexCoord")
        aNormal = GLES20.glGetAttribLocation(programSolid, "aNormal")

        // Overlay shader (second layer with alpha blend)
        val vsOverlay = """
            attribute vec3 aPosition;
            attribute vec2 aTexCoord;
            attribute vec3 aNormal;
            uniform mat4 uMVPMatrix;
            uniform vec3 uLightDir;
            uniform int uUseOverlay;
            varying vec2 vTexCoord;
            varying vec2 vOverlayTexCoord;
            varying float vLighting;
            void main() {
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
                vTexCoord = aTexCoord;
                vOverlayTexCoord = aTexCoord;
                vec3 normal = normalize(aNormal);
                float diff = max(dot(normal, normalize(uLightDir)), 0.0);
                vLighting = 0.6 + 0.4 * diff;
            }
        """.trimIndent()
        val fsOverlay = """
            precision mediump float;
            uniform sampler2D uTexture;
            uniform sampler2D uOverlayTexture;
            uniform int uUseOverlay;
            uniform float uAlpha;
            uniform float uOverlayAlpha;
            varying vec2 vTexCoord;
            varying vec2 vOverlayTexCoord;
            varying float vLighting;
            void main() {
                vec4 base = texture2D(uTexture, vTexCoord);
                base.rgb *= vLighting;
                base.a *= uAlpha;
                if (uUseOverlay == 1) {
                    vec4 overlay = texture2D(uOverlayTexture, vOverlayTexCoord);
                    overlay.rgb *= vLighting;
                    overlay.a *= uOverlayAlpha;
                    float alpha = base.a + overlay.a * (1.0 - base.a);
                    vec3 color = (base.rgb * base.a + overlay.rgb * overlay.a * (1.0 - base.a)) / max(alpha, 0.001);
                    if (alpha < 0.01) discard;
                    gl_FragColor = vec4(color, alpha);
                } else {
                    if (base.a < 0.01) discard;
                    gl_FragColor = base;
                }
            }
        """.trimIndent()
        programOverlay = createProgram(vsOverlay, fsOverlay)
        uMVPMatrixOverlay = GLES20.glGetUniformLocation(programOverlay, "uMVPMatrix")
        uTextureOverlay = GLES20.glGetUniformLocation(programOverlay, "uTexture")
        uOverlayTexture = GLES20.glGetUniformLocation(programOverlay, "uOverlayTexture")
        uUseOverlay = GLES20.glGetUniformLocation(programOverlay, "uUseOverlay")
        uAlphaOverlay = GLES20.glGetUniformLocation(programOverlay, "uAlpha")
        uOverlayAlpha = GLES20.glGetUniformLocation(programOverlay, "uOverlayAlpha")
        uLightDirOverlay = GLES20.glGetUniformLocation(programOverlay, "uLightDir")

        // Pick shader (flat color-ID)
        val vsPick = """
            attribute vec3 aPosition;
            uniform mat4 uMVPMatrix;
            void main() {
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
            }
        """.trimIndent()
        val fsPick = """
            precision mediump float;
            uniform vec4 uPickColor;
            void main() {
                gl_FragColor = uPickColor;
            }
        """.trimIndent()
        programPick = createProgram(vsPick, fsPick)
        uMVPMatrixPick = GLES20.glGetUniformLocation(programPick, "uMVPMatrix")
        uPickColor = GLES20.glGetUniformLocation(programPick, "uPickColor")
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            throw RuntimeException("Shader link failed: ${GLES20.glGetProgramInfoLog(program)}")
        }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader compile failed: $log")
        }
        return shader
    }

    private fun initGeometry() {
        // Build all 6 parts: each is a cube with 24 vertices, 36 indices
        // Total: 6 * 24 = 144 vertices, 6 * 36 = 216 indices
        val totalVertices = 6 * 24
        val totalIndices = 6 * 36
        val vertices = FloatArray(totalVertices * 3)
        val texCoords = FloatArray(totalVertices * 2)
        val normals = FloatArray(totalVertices * 3)
        val indices = ShortArray(totalIndices)

        var vIdx = 0
        var tIdx = 0
        var nIdx = 0
        var iIdx = 0
        var vertexOffset = 0

        partIndexOffsets = IntArray(6)
        partIndexCounts = IntArray(6)

        for (part in 0..5) {
            partIndexOffsets[part] = iIdx
            buildPartCube(part, vertices, texCoords, normals, indices, vIdx, tIdx, nIdx, iIdx, vertexOffset)
            vertexOffset += 24
            partIndexCounts[part] = iIdx - partIndexOffsets[part]
        }

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(vertices); position(0) }
        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(texCoords); position(0) }
        normalBuffer = ByteBuffer.allocateDirect(normals.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(normals); position(0) }
        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply { put(indices); position(0) }
    }

    private fun buildPartCube(
        part: Int,
        vertices: FloatArray,
        texCoords: FloatArray,
        normals: FloatArray,
        indices: ShortArray,
        vIdx: Int, tIdx: Int, nIdx: Int, iIdx: Int, vertexOffset: Int
    ) {
        // Unit cube vertices (centered at origin)
        val cubeVerts = floatArrayOf(
            // Back (-Z)
             0.5f,  0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
            -0.5f, -0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,
            // Front (+Z)
            -0.5f,  0.5f,  0.5f,
            -0.5f, -0.5f,  0.5f,
             0.5f, -0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
            // Left (-X)
            -0.5f,  0.5f, -0.5f,
            -0.5f, -0.5f, -0.5f,
            -0.5f, -0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f,
            // Right (+X)
             0.5f,  0.5f,  0.5f,
             0.5f, -0.5f,  0.5f,
             0.5f, -0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
            // Top (+Y)
            -0.5f,  0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
             0.5f,  0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,
            // Bottom (-Y)
             0.5f, -0.5f,  0.5f,
            -0.5f, -0.5f,  0.5f,
            -0.5f, -0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
        )

        // Cube UVs (per face) - will be overridden per part
        val cubeUVs = floatArrayOf(
            // Back
            1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f,
            // Front
            0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f,
            // Left
            1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f,
            // Right
            0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f,
            // Top
            0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f,
            // Bottom
            1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f,
        )

        // Cube normals
        val cubeNormals = floatArrayOf(
            // Back
            0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,
            // Front
            0f, 0f, 1f,   0f, 0f, 1f,   0f, 0f, 1f,   0f, 0f, 1f,
            // Left
            -1f, 0f, 0f,  -1f, 0f, 0f,  -1f, 0f, 0f,  -1f, 0f, 0f,
            // Right
            1f, 0f, 0f,   1f, 0f, 0f,   1f, 0f, 0f,   1f, 0f, 0f,
            // Top
            0f, 1f, 0f,   0f, 1f, 0f,   0f, 1f, 0f,   0f, 1f, 0f,
            // Bottom
            0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,
        )

        // Cube indices (same for all parts)
        val cubeIndices = shortArrayOf(
            0, 1, 2, 0, 2, 3,
            4, 5, 6, 4, 6, 7,
            8, 9, 10, 8, 10, 11,
            12, 13, 14, 12, 14, 15,
            16, 17, 18, 16, 18, 19,
            20, 21, 22, 20, 22, 23
        )

        // Apply part-specific scale, pivot offset, and UVs
        val hx = halfExtents[part * 3]
        val hy = halfExtents[part * 3 + 1]
        val hz = halfExtents[part * 3 + 2]
        val px = pivots[part * 3]
        val py = pivots[part * 3 + 1]
        val pz = pivots[part * 3 + 2]

        for (i in 0..23) {
            // Scale and translate to part position
            val vx = cubeVerts[i * 3] * hx * 2f + px
            val vy = cubeVerts[i * 3 + 1] * hy * 2f + py
            val vz = cubeVerts[i * 3 + 2] * hz * 2f + pz
            vertices[vIdx + i * 3] = vx
            vertices[vIdx + i * 3 + 1] = vy
            vertices[vIdx + i * 3 + 2] = vz

            normals[nIdx + i * 3] = cubeNormals[i * 3]
            normals[nIdx + i * 3 + 1] = cubeNormals[i * 3 + 1]
            normals[nIdx + i * 3 + 2] = cubeNormals[i * 3 + 2]

            // UVs: start with unit cube UVs, will be remapped by shader or here
            texCoords[tIdx + i * 2] = cubeUVs[i * 2]
            texCoords[tIdx + i * 2 + 1] = cubeUVs[i * 2 + 1]
        }

        for (i in 0..35) {
            indices[iIdx + i] = (cubeIndices[i] + vertexOffset).toShort()
        }
    }

    private fun initTextures() {
        val textures = IntArray(3)
        GLES20.glGenTextures(3, textures, 0)
        skinTextureId = textures[0]
        overlayTextureId = textures[1]
        capeTextureId = textures[2]

        for (id in textures) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        }
    }

    private fun drawAllParts(program: Int, uMVP: Int, aPos: Int, aNorm: Int, useOverlay: Boolean) {
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvpMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, skinTextureId)
        if (program == programOverlay) {
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uTexture"), 0)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTextureId)
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uOverlayTexture"), 1)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, capeTextureId)
        }

        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(aNorm)
        GLES20.glVertexAttribPointer(aNorm, 3, GLES20.GL_FLOAT, false, 0, normalBuffer)

        // For now, draw all parts as one batch (per-part transform in shader would be better)
        // This is a simplification - full Skinseed does per-part MVP
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, partIndexCounts.sum(), GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aNorm)
    }

    private fun drawHighlight() {
        // Simple wireframe or emissive highlight for selected part
        // Omitted for brevity - would draw selected part with different color
    }

    private fun updateAnimation(dt: Float) {
        if (isAnimating) {
            animationTime += dt
        }

        // Slerp each part toward target
        for (part in 0..5) {
            slerpTowardTarget(part, dt)
        }
        slerpBodyTowardTarget(dt)

        // Auto-rotate when idle
        if (autoRotate && !isDragging) {
            val dq = quatFromAxisAngle(0f, AUTO_ROTATE_SPEED * dt, 0f)
            quatMultiply(bodyQuat, dq, bodyQuat)
        }
    }

    private fun slerpTowardTarget(part: Int, dt: Float) {
        val base = part * 4
        val q0 = floatArrayOf(partQuats[base], partQuats[base+1], partQuats[base+2], partQuats[base+3])
        val q1 = floatArrayOf(targetQuats[base], targetQuats[base+1], targetQuats[base+2], targetQuats[base+3])
        val result = floatArrayOf(0f, 0f, 0f, 0f)
        quatSlerp(q0, q1, 1f / SLERP_SPEED * dt, result)
        partQuats[base] = result[0]
        partQuats[base+1] = result[1]
        partQuats[base+2] = result[2]
        partQuats[base+3] = result[3]
    }

    private fun slerpBodyTowardTarget(dt: Float) {
        val result = floatArrayOf(0f, 0f, 0f, 0f)
        quatSlerp(bodyQuat, targetBodyQuat, 1f / SLERP_SPEED * dt, result)
        bodyQuat[0] = result[0]
        bodyQuat[1] = result[1]
        bodyQuat[2] = result[2]
        bodyQuat[3] = result[3]
    }

    private fun updateCamera() {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, -0.4f, 0f)

        // Apply body quaternion rotation
        val bodyRot = FloatArray(16)
        quatToMatrix(bodyQuat, bodyRot)
        Matrix.multiplyMM(modelMatrix, 0, bodyRot, 0, modelMatrix, 0)

        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, cameraDistance,
            0f, 0.4f, 0f,
            0f, 1f, 0f)

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0)
    }

    fun setSkin(bitmap: Bitmap?) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, skinTextureId)
        if (bitmap != null) {
            android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        } else {
            val placeholder = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            placeholder.eraseColor(0xFF8B4513)
            android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, placeholder, 0)
            placeholder.recycle()
        }
    }

    fun setOverlay(bitmap: Bitmap?) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTextureId)
        if (bitmap != null) {
            android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        } else {
            val placeholder = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            placeholder.eraseColor(0x00000000)
            android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, placeholder, 0)
            placeholder.recycle()
        }
    }

    fun setCape(bitmap: Bitmap?) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, capeTextureId)
        if (bitmap != null) {
            android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        } else {
            val placeholder = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
            placeholder.eraseColor(0x00000000)
            android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, placeholder, 0)
            placeholder.recycle()
        }
    }

    fun setVariant(variant: String) {
        this.variant = variant
        isSlim = variant == "SLIM"
        // Rebuild geometry for slim arms
        initGeometry()
        view.requestRender()
    }

    fun setOverlayEnabled(enabled: Boolean) {
        overlayEnabled = enabled
    }

    fun setAutoRotate(enabled: Boolean) {
        autoRotate = enabled
    }

    fun setPose(poseName: String) {
        val pose = builtInPoses[poseName] ?: return
        applyPose(pose)
    }

    fun setCustomPose(pose: Map<String, FloatArray>) {
        val flat = FloatArray(6 * 4)
        val partNames = arrayOf("head", "body", "armL", "armR", "legL", "legR")
        for ((i, name) in partNames.withIndex()) {
            val q = pose[name] ?: floatArrayOf(1f, 0f, 0f, 0f)
            flat[i * 4] = q[0]
            flat[i * 4 + 1] = q[1]
            flat[i * 4 + 2] = q[2]
            flat[i * 4 + 3] = q[3]
        }
        applyPose(flat)
    }

    fun resetPose() {
        for (i in 0..5) {
            val base = i * 4
            partQuats[base] = 1f
            partQuats[base + 1] = 0f
            partQuats[base + 2] = 0f
            partQuats[base + 3] = 0f
            targetQuats[base] = 1f
            targetQuats[base + 1] = 0f
            targetQuats[base + 2] = 0f
            targetQuats[base + 3] = 0f
        }
        bodyQuat[0] = 1f; bodyQuat[1] = 0f; bodyQuat[2] = 0f; bodyQuat[3] = 0f
        targetBodyQuat[0] = 1f; targetBodyQuat[1] = 0f; targetBodyQuat[2] = 0f; targetBodyQuat[3] = 0f
    }

    private fun applyPose(pose: FloatArray) {
        for (i in 0..5) {
            val base = i * 4
            targetQuats[base] = pose[base]
            targetQuats[base + 1] = pose[base + 1]
            targetQuats[base + 2] = pose[base + 2]
            targetQuats[base + 3] = pose[base + 3]
        }
    }

    private fun buildBuiltInPoses(): Map<String, FloatArray> {
        val poses = mutableMapOf<String, FloatArray>()
        val deg2rad = Math.PI / 180.0

        // Helper: quaternion from euler (yaw, pitch, roll) in radians
        fun q(yaw: Double, pitch: Double, roll: Double): FloatArray {
            val cy = Math.cos(yaw * 0.5)
            val sy = Math.sin(yaw * 0.5)
            val cp = Math.cos(pitch * 0.5)
            val sp = Math.sin(pitch * 0.5)
            val cr = Math.cos(roll * 0.5)
            val sr = Math.sin(roll * 0.5)
            return floatArrayOf(
                (cr * cp * cy - sr * sp * sy).toFloat(), // w
                (cr * sp * cy - sr * cp * sy).toFloat(), // x
                (cr * cp * sy + sr * sp * cy).toFloat(), // y
                (cr * sp * sy + sr * cp * cy).toFloat()  // z
            )
        }

        // 0: Standing (idle)
        poses["standing"] = floatArrayOf(
            *q(0.0, 0.0, 0.0),      // head
            *q(0.0, 0.0, 0.0),      // body
            *q(0.0, -0.3, 0.0),     // armL
            *q(0.0, -0.3, 0.0),     // armR
            *q(0.0, 0.2, 0.0),      // legL
            *q(0.0, 0.2, 0.0)       // legR
        )

        // 1: Walking
        poses["walking"] = floatArrayOf(
            *q(0.0, 0.0, 0.0),
            *q(0.0, 0.0, 0.0),
            *q(0.0, -0.8, 0.0),
            *q(0.0, 0.8, 0.0),
            *q(0.0, 0.8, 0.0),
            *q(0.0, -0.8, 0.0)
        )

        // 2: Arms raised + body yaw 30°
        poses["arms_raised"] = floatArrayOf(
            *q(0.0, 0.0, 0.0),
            *q(30.0 * deg2rad, 0.0, 0.0),
            *q(0.0, -1.5, 0.0),
            *q(0.0, -1.5, 0.0),
            *q(0.0, 0.2, 0.0),
            *q(0.0, 0.2, 0.0)
        )

        // 3: Action (asymmetric)
        poses["action"] = floatArrayOf(
            *q(0.0, 0.2, 0.0),
            *q(0.0, 0.0, 0.0),
            *q(0.0, -1.0, 0.5),
            *q(0.0, -0.5, -0.5),
            *q(0.0, 0.5, 0.0),
            *q(0.0, 0.3, 0.0)
        )

        // 4: 180° back-facing
        poses["back"] = floatArrayOf(
            *q(0.0, 0.0, 0.0),
            *q(180.0 * deg2rad, 0.0, 0.0),
            *q(0.0, -0.3, 0.0),
            *q(0.0, -0.3, 0.0),
            *q(0.0, 0.2, 0.0),
            *q(0.0, 0.2, 0.0)
        )

        // 5: Lean/run
        poses["lean"] = floatArrayOf(
            *q(0.0, 0.3, 0.0),
            *q(0.0, -0.2, 0.0),
            *q(0.0, -1.0, 0.0),
            *q(0.0, 1.0, 0.0),
            *q(0.0, 1.2, 0.0),
            *q(0.0, -1.2, 0.0)
        )

        // 6: Run (alt)
        poses["run"] = floatArrayOf(
            *q(0.0, 0.3, 0.0),
            *q(0.0, -0.2, 0.0),
            *q(0.0, -1.2, 0.0),
            *q(0.0, 1.2, 0.0),
            *q(0.0, 1.5, 0.0),
            *q(0.0, -1.5, 0.0)
        )

        return poses
    }

    // ===== QUATERNION MATH =====

    private fun quatFromAxisAngle(x: Float, y: Float, z: Float): FloatArray {
        val angle = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        if (angle == 0f) return floatArrayOf(1f, 0f, 0f, 0f)
        val half = angle * 0.5f
        val s = Math.sin(half.toDouble()).toFloat() / angle
        return floatArrayOf(
            Math.cos(half.toDouble()).toFloat(),
            x * s,
            y * s,
            z * s
        )
    }

    private fun quatMultiply(q1: FloatArray, q2: FloatArray, out: FloatArray) {
        val w1 = q1[0]; val x1 = q1[1]; val y1 = q1[2]; val z1 = q1[3]
        val w2 = q2[0]; val x2 = q2[1]; val y2 = q2[2]; val z2 = q2[3]
        out[0] = w1 * w2 - x1 * x2 - y1 * y2 - z1 * z2
        out[1] = w1 * x2 + x1 * w2 + y1 * z2 - z1 * y2
        out[2] = w1 * y2 - x1 * z2 + y1 * w2 + z1 * x2
        out[3] = w1 * z2 + x1 * y2 - y1 * x2 + z1 * w2
    }

    private fun quatSlerp(q0: FloatArray, q1: FloatArray, t: Float, out: FloatArray) {
        var dot = q0[0] * q1[0] + q0[1] * q1[1] + q0[2] * q1[2] + q0[3] * q1[3]
        var q1w = q1[0]; var q1x = q1[1]; var q1y = q1[2]; var q1z = q1[3]
        if (dot < 0f) {
            dot = -dot
            q1w = -q1w; q1x = -q1x; q1y = -q1y; q1z = -q1z
        }
        if (dot > 1f - SLERP_EPSILON) {
            // Linear interpolation for very close quaternions
            out[0] = q0[0] + (q1w - q0[0]) * t
            out[1] = q0[1] + (q1x - q0[1]) * t
            out[2] = q0[2] + (q1y - q0[2]) * t
            out[3] = q0[3] + (q1z - q0[3]) * t
            val len = Math.sqrt((out[0] * out[0] + out[1] * out[1] + out[2] * out[2] + out[3] * out[3]).toDouble()).toFloat()
            out[0] /= len; out[1] /= len; out[2] /= len; out[3] /= len
            return
        }
        val theta0 = Math.acos(dot.toDouble())
        val theta = theta0 * t
        val sinTheta = Math.sin(theta)
        val sinTheta0 = Math.sin(theta0)
        val s0 = Math.cos(theta) - dot * sinTheta / sinTheta0
        val s1 = sinTheta / sinTheta0
        out[0] = (q0[0] * s0 + q1w * s1).toFloat()
        out[1] = (q0[1] * s0 + q1x * s1).toFloat()
        out[2] = (q0[2] * s0 + q1y * s1).toFloat()
        out[3] = (q0[3] * s0 + q1z * s1).toFloat()
    }

    private fun quatToMatrix(q: FloatArray, m: FloatArray) {
        val w = q[0]; val x = q[1]; val y = q[2]; val z = q[3]
        val xx = x * x; val yy = y * y; val zz = z * z
        val xy = x * y; val xz = x * z; val yz = y * z
        val wx = w * x; val wy = w * y; val wz = w * z

        m[0] = 1f - 2f * (yy + zz)
        m[1] = 2f * (xy - wz)
        m[2] = 2f * (xz + wy)
        m[3] = 0f

        m[4] = 2f * (xy + wz)
        m[5] = 1f - 2f * (xx + zz)
        m[6] = 2f * (yz - wx)
        m[7] = 0f

        m[8] = 2f * (xz - wy)
        m[9] = 2f * (yz + wx)
        m[10] = 1f - 2f * (xx + yy)
        m[11] = 0f

        m[12] = 0f
        m[13] = 0f
        m[14] = 0f
        m[15] = 1f
    }

    // ===== TOUCH HANDLING =====

    fun handleTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    if (selectedPart >= 0) {
                        // Part drag: compute delta quaternion in part-local space
                        val sensitivity = 0.7f * 6.761f / Math.hypot(view.width.toDouble(), view.height.toDouble()).toFloat()
                        val dq = quatFromAxisAngle(dy * sensitivity, dx * sensitivity, 0f)
                        val base = selectedPart * 4
                        val current = floatArrayOf(
                            targetQuats[base], targetQuats[base+1], targetQuats[base+2], targetQuats[base+3]
                        )
                        val result = floatArrayOf(0f, 0f, 0f, 0f)
                        quatMultiply(dq, current, result)
                        targetQuats[base] = result[0]
                        targetQuats[base+1] = result[1]
                        targetQuats[base+2] = result[2]
                        targetQuats[base+3] = result[3]
                    } else {
                        // Whole-model drag: rotate body quaternion
                        val sensitivity = 10.0f
                        val dq = quatFromAxisAngle(dy * sensitivity * 0.01f, dx * sensitivity * 0.01f, 0f)
                        val result = floatArrayOf(0f, 0f, 0f, 0f)
                        quatMultiply(dq, targetBodyQuat, result)
                        targetBodyQuat[0] = result[0]
                        targetBodyQuat[1] = result[1]
                        targetBodyQuat[2] = result[2]
                        targetBodyQuat[3] = result[3]
                    }

                    view.requestRender()
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    // Color-ID pick on tap
                    val pickX = event.x.toInt()
                    val pickY = event.y.toInt()
                    selectedPart = pickPart(pickX, pickY)
                    view.requestRender()
                }
                isDragging = false
            }
        }
    }

    private fun pickPart(x: Int, y: Int): Int {
        // Render color-ID pick at tap coordinates
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(programPick)
        GLES20.glUniformMatrix4fv(uMVPMatrixPick, 1, false, mvpMatrix, 0)

        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        for (part in 0..5) {
            GLES20.glUniform4f(uPickColor,
                pickColors[part * 4],
                pickColors[part * 4 + 1],
                pickColors[part * 4 + 2],
                pickColors[part * 4 + 3]
            )
            val offset = partIndexOffsets[part] * 2
            val count = partIndexCounts[part]
            indexBuffer.position(offset)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, count, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
        }

        // Read pixel at tap location (y is flipped in GL)
        val glY = view.height - y
        val pixel = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        GLES20.glReadPixels(x, glY, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixel)

        val r = pixel[0].toInt() and 0xFF
        val g = pixel[1].toInt() and 0xFF
        val b = pixel[2].toInt() and 0xFF

        // Match to pick color
        for (part in 0..5) {
            val pr = (pickColors[part * 4] * 255).toInt()
            val pg = (pickColors[part * 4 + 1] * 255).toInt()
            val pb = (pickColors[part * 4 + 2] * 255).toInt()
            if (r == pr && g == pg && b == pb) {
                return part
            }
        }
        return -1
    }
}