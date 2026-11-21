/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * 3D MODEL PREVIEW — OpenGL ES 2.0 renderer for Minecraft character models.
 * Renders Steve (CLASSIC) or Alex (SLIM) skins with overlay layers, arm/leg swing,
 * head idle animation, and interactive camera controls.
 */
package me.shadow.eclipselauncher.ui.character

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.AttributeSet
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * GLSurfaceView that renders a Minecraft character model with skin.
 * Supports CLASSIC (Steve, 4px arms) and SLIM (Alex, 3px arms) variants,
 * overlay layers (jacket/sleeves/pants), and interactive camera controls.
 */
class ModelPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {

    private val renderer: ModelRenderer

    init {
        setEGLContextClientVersion(2)
        setPreserveEGLContextOnPause(true)
        renderer = ModelRenderer(this)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    /** Updates the skin texture from a bitmap. */
    fun setSkin(bitmap: Bitmap?) {
        queueEvent { renderer.setSkin(bitmap) }
        requestRender()
    }

    /** Updates the overlay texture (jacket/sleeves/pants). */
    fun setOverlay(bitmap: Bitmap?) {
        queueEvent { renderer.setOverlay(bitmap) }
        requestRender()
    }

    /** Sets the model variant: "CLASSIC" (Steve, 4px) or "SLIM" (Alex, 3px). */
    fun setVariant(variant: String) {
        queueEvent { renderer.setVariant(variant) }
        requestRender()
    }

    /** Enables/disables the overlay layer. */
    fun setOverlayEnabled(enabled: Boolean) {
        queueEvent { renderer.setOverlayEnabled(enabled) }
        requestRender()
    }

    /** Starts/stops the idle animation loop. */
    fun setAnimating(animating: Boolean) {
        renderer.isAnimating = animating
        if (animating) requestRender()
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        renderer.handleTouch(event)
        return true
    }
}

/**
 * Core renderer implementing the OpenGL ES 2.0 pipeline.
 * Handles model geometry, skin textures, animation, and camera.
 */
class ModelRenderer(private val view: ModelPreviewView) : GLSurfaceView.Renderer {

    // Shader program handles
    private var program: Int = 0
    private var uMVPMatrix: Int = 0
    private var uTexture: Int = 0
    private var uOverlayTexture: Int = 0
    private var uUseOverlay: Int = 0
    private var uAlpha: Int = 0
    private var uOverlayAlpha: Int = 0
    private var uLightDir: Int = 0

    // Attribute locations
    private var aPosition: Int = 0
    private var aTexCoord: Int = 0
    private var aNormal: Int = 0

    // Geometry buffers
    private var vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var texCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var normalBuffer: FloatBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var indexBuffer: ShortBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asShortBuffer()
    private var indexCount: Int = 0

    // Textures
    private var skinTextureId: Int = -1
    private var overlayTextureId: Int = -1

    // Camera
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private var cameraDistance = 2.5f
    private var cameraYaw = 0.0f
    private var cameraPitch = -0.2f

    // Touch
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    // Animation
    var isAnimating = true
    private var lastFrameTime = System.nanoTime()
    private var animationTime = 0f

    // Model state
    var variant = "CLASSIC"
    var overlayEnabled = true
    private var isSlim = false

    // Animation parameters
    private var armSwing = 0f
    private var legSwing = 0f
    private var headYaw = 0f
    private var headPitch = 0f
    private var bodyPitch = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.15f, 0.15f, 0.18f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFuncSeparate(
            GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA,
            GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA
        )

        initShaders()
        initGeometry()
        initTextures()
        updateVariant()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 45f, width.toFloat() / height.toFloat(), 0.1f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        updateAnimation()
        updateCamera()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        GLES20.glUseProgram(program)

        // MVP matrix
        GLES20.glUniformMatrix4fv(uMVPMatrix, 1, false, mvpMatrix, 0)

        // Textures
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, skinTextureId)
        GLES20.glUniform1i(uTexture, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTextureId)
        GLES20.glUniform1i(uOverlayTexture, 1)

        GLES20.glUniform1i(uUseOverlay, if (overlayEnabled) 1 else 0)
        GLES20.glUniform1f(uAlpha, 1f)
        GLES20.glUniform1f(uOverlayAlpha, 1f)

        // Light direction (from top-right-back)
        GLES20.glUniform3f(uLightDir, 0.5f, 1f, -0.5f)

        // Bind attributes
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(aTexCoord)
        GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glEnableVertexAttribArray(aNormal)
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 0, normalBuffer)

        // Draw
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        // Cleanup
        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aTexCoord)
        GLES20.glDisableVertexAttribArray(aNormal)
        GLES20.glUseProgram(0)
    }

    private fun initShaders() {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_SOURCE)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SOURCE)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            throw RuntimeException("Shader link failed: ${GLES20.glGetProgramInfoLog(program)}")
        }

        // Uniforms
        uMVPMatrix = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        uTexture = GLES20.glGetUniformLocation(program, "uTexture")
        uOverlayTexture = GLES20.glGetUniformLocation(program, "uOverlayTexture")
        uUseOverlay = GLES20.glGetUniformLocation(program, "uUseOverlay")
        uAlpha = GLES20.glGetUniformLocation(program, "uAlpha")
        uOverlayAlpha = GLES20.glGetUniformLocation(program, "uOverlayAlpha")
        uLightDir = GLES20.glGetUniformLocation(program, "uLightDir")

        // Attributes
        aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord")
        aNormal = GLES20.glGetAttribLocation(program, "aNormal")

        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
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
        // Build model geometry (same as vanilla Minecraft)
        val parts = buildModelParts()
        val vertices = FloatArray(parts.sumOf { it.vertices.size })
        val texCoords = FloatArray(parts.sumOf { it.texCoords.size })
        val normals = FloatArray(parts.sumOf { it.normals.size })
        val indices = ShortArray(parts.sumOf { it.indices.size })

        var vIdx = 0
        var tIdx = 0
        var nIdx = 0
        var iIdx = 0
        var vertexOffset = 0

        for (part in parts) {
            System.arraycopy(part.vertices, 0, vertices, vIdx, part.vertices.size)
            vIdx += part.vertices.size
            System.arraycopy(part.texCoords, 0, texCoords, tIdx, part.texCoords.size)
            tIdx += part.texCoords.size
            System.arraycopy(part.normals, 0, normals, nIdx, part.normals.size)
            nIdx += part.normals.size

            for (idx in part.indices) {
                indices[iIdx] = (idx + vertexOffset).toShort()
                iIdx++
            }
            vertexOffset += part.vertices.size / 3
        }

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(vertices); position(0) }
        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(texCoords); position(0) }
        normalBuffer = ByteBuffer.allocateDirect(normals.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(normals); position(0) }
        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply { put(indices); position(0) }
        indexCount = indices.size
    }

    private fun initTextures() {
        val textures = IntArray(2)
        GLES20.glGenTextures(2, textures, 0)
        skinTextureId = textures[0]
        overlayTextureId = textures[1]

        for (id in textures) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        }
    }

    private fun updateVariant() {
        isSlim = variant == "SLIM"
        initGeometry()
        view.requestRender()
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

    fun setVariant(variant: String) {
        this.variant = variant
        view.queueEvent { updateVariant() }
    }

    fun setOverlayEnabled(enabled: Boolean) {
        overlayEnabled = enabled
    }

    private fun updateAnimation() {
        val now = System.nanoTime()
        val dt = (now - lastFrameTime) / 1e9f
        lastFrameTime = now

        if (isAnimating) {
            animationTime += dt
        }

        armSwing = kotlin.math.sin(animationTime * 6f) * 0.8f
        legSwing = -armSwing

        headYaw = kotlin.math.sin(animationTime * 0.7f) * 0.3f
        headPitch = kotlin.math.sin(animationTime * 1.1f) * 0.15f
    }

    private fun updateCamera() {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, -0.8f, 0f)
        Matrix.rotateM(modelMatrix, 0, -cameraPitch * (180f / kotlin.math.PI.toFloat()), 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, cameraYaw * (180f / kotlin.math.PI.toFloat()), 0f, 1f, 0f)

        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, cameraDistance,
            0f, 0f, 0f,
            0f, 1f, 0f)

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0)
    }

    fun handleTouch(event: android.view.MotionEvent) {
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    cameraYaw += dx * 0.01f
                    cameraPitch = (cameraPitch - dy * 0.01f).coerceIn(-1.2f, 0.5f)
                    view.requestRender()
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }
            android.view.MotionEvent.ACTION_UP,
            android.view.MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
    }

    // ===== MODEL GEOMETRY =====

    private data class ModelPart(
        val vertices: FloatArray,
        val texCoords: FloatArray,
        val normals: FloatArray,
        val indices: ShortArray
    )

    // UV helper - defined once at class level
    private fun uv(u: Int, v: Int): Pair<Float, Float> = (u / 64f) to (v / 64f)

    // Helper to create map entries with explicit parentheses for type inference
    private inline fun <K, V> entry(k: K, v: V): Pair<K, V> = k to v

    private fun buildModelParts(): List<ModelPart> {
        val parts = mutableListOf<ModelPart>()

        // Texture coordinates for each face of each part
        // Vanilla Minecraft skin layout (64x64)
        val uv = { u: Int, v: Int -> (u / 64f) to (v / 64f) }

        // Helper to create UV map entries with explicit parentheses
        fun e(k: String, u1: Int, v1: Int, u2: Int, v2: Int) = entry(k, entry(uv(u1, v1), uv(u2, v2)))

        val headUV = mapOf(
            e("front", 8, 8, 16, 16),
            e("back", 24, 8, 32, 16),
            e("right", 0, 8, 8, 16),
            e("left", 16, 8, 24, 16),
            e("top", 8, 0, 16, 8),
            e("bottom", 16, 0, 24, 8)
        )
        val hatUV = mapOf(
            e("front", 40, 8, 48, 16),
            e("back", 56, 8, 64, 16),
            e("right", 32, 8, 40, 16),
            e("left", 48, 8, 56, 16),
            e("top", 40, 0, 48, 8),
            e("bottom", 48, 0, 56, 8)
        )
        val bodyUV = mapOf(
            e("front", 20, 20, 28, 32),
            e("back", 32, 20, 40, 32),
            e("right", 16, 20, 20, 32),
            e("left", 28, 20, 32, 32),
            e("top", 20, 16, 28, 20),
            e("bottom", 28, 16, 36, 20)
        )
        val jacketUV = mapOf(
            e("front", 20, 36, 28, 48),
            e("back", 32, 36, 40, 48),
            e("right", 16, 36, 20, 48),
            e("left", 28, 36, 32, 48),
            e("top", 20, 32, 28, 36),
            e("bottom", 28, 32, 36, 36)
        )
        val armRUV = if (isSlim) mapOf(
            e("front", 44, 20, 47, 32),
            e("back", 51, 20, 54, 32),
            e("right", 40, 20, 43, 32),
            e("left", 47, 20, 50, 32),
            e("top", 44, 16, 47, 20),
            e("bottom", 47, 16, 50, 20)
        ) else mapOf(
            e("front", 44, 20, 48, 32),
            e("back", 52, 20, 56, 32),
            e("right", 40, 20, 44, 32),
            e("left", 48, 20, 52, 32),
            e("top", 44, 16, 48, 20),
            e("bottom", 48, 16, 52, 20)
        )
        val sleeveRUV = if (isSlim) mapOf(
            e("front", 44, 36, 47, 48),
            e("back", 51, 36, 54, 48),
            e("right", 40, 36, 43, 48),
            e("left", 47, 36, 50, 48),
            e("top", 44, 32, 47, 36),
            e("bottom", 47, 32, 50, 36)
        ) else mapOf(
            e("front", 44, 36, 48, 48),
            e("back", 52, 36, 56, 48),
            e("right", 40, 36, 44, 48),
            e("left", 48, 36, 52, 48),
            e("top", 44, 32, 48, 36),
            e("bottom", 48, 32, 52, 36)
        )
        val armLUV = if (isSlim) mapOf(
            e("front", 36, 52, 39, 64),
            e("back", 43, 52, 46, 64),
            e("right", 32, 52, 35, 64),
            e("left", 39, 52, 42, 64),
            e("top", 36, 48, 39, 52),
            e("bottom", 39, 48, 42, 52)
        ) else mapOf(
            e("front", 36, 52, 40, 64),
            e("back", 44, 52, 48, 64),
            e("right", 32, 52, 36, 64),
            e("left", 40, 52, 44, 64),
            e("top", 36, 48, 40, 52),
            e("bottom", 40, 48, 44, 52)
        )
        val sleeveLUV = if (isSlim) mapOf(
            e("front", 52, 52, 55, 64),
            e("back", 59, 52, 62, 64),
            e("right", 48, 52, 51, 64),
            e("left", 55, 52, 58, 64),
            e("top", 52, 48, 55, 52),
            e("bottom", 55, 48, 58, 52)
        ) else mapOf(
            e("front", 52, 52, 56, 64),
            e("back", 60, 52, 64, 64),
            e("right", 48, 52, 52, 64),
            e("left", 56, 52, 60, 64),
            e("top", 52, 48, 56, 52),
            e("bottom", 56, 48, 60, 52)
        )
        val legRUV = mapOf(
            e("front", 4, 20, 8, 32),
            e("back", 12, 20, 16, 32),
            e("right", 0, 20, 4, 32),
            e("left", 8, 20, 12, 32),
            e("top", 4, 16, 8, 20),
            e("bottom", 8, 16, 12, 20)
        )
        val pantsRUV = mapOf(
            e("front", 4, 36, 8, 48),
            e("back", 12, 36, 16, 48),
            e("right", 0, 36, 4, 48),
            e("left", 8, 36, 12, 48),
            e("top", 4, 32, 8, 36),
            e("bottom", 8, 32, 12, 36)
        )
        val legLUV = mapOf(
            e("front", 20, 52, 24, 64),
            e("back", 28, 52, 32, 64),
            e("right", 16, 52, 20, 64),
            e("left", 24, 52, 28, 64),
            e("top", 20, 48, 24, 52),
            e("bottom", 24, 48, 28, 52)
        )
        val pantsLUV = mapOf(
            e("front", 4, 52, 8, 64),
            e("back", 12, 52, 16, 64),
            e("right", 0, 52, 4, 64),
            e("left", 8, 52, 12, 64),
            e("top", 4, 48, 8, 52),
            e("bottom", 8, 48, 12, 52)
        )

        // Part dimensions
        val headSize = 8f
        val bodyWidth = 8f
        val bodyHeight = 12f
        val bodyDepth = 4f
        val armWidth = if (isSlim) 3f else 4f
        val armHeight = 12f
        val armDepth = 4f
        val legWidth = 4f
        val legHeight = 12f
        val legDepth = 4f

        fun addBox(
            x0: Float, y0: Float, z0: Float,
            x1: Float, y1: Float, z1: Float,
            uvMap: Map<String, Pair<Pair<Float, Float>, Pair<Float, Float>>>,
            inflate: Float = 0f
        ): ModelPart {
            val verts = FloatArray(24)
            val texs = FloatArray(16)
            val norms = FloatArray(24)
            val inds = shortArrayOf(0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7, 8, 9, 10, 8, 10, 11, 12, 13, 14, 12, 14, 15, 16, 17, 18, 16, 18, 19, 20, 21, 22, 20, 22, 23)

            val fx0 = x0 - inflate
            val fy0 = y0 - inflate
            val fz0 = z0 - inflate
            val fx1 = x1 + inflate
            val fy1 = y1 + inflate
            val fz1 = z1 + inflate

            // Front face (+Z)
            verts[0] = x0 - inflate; verts[1] = y1 + inflate; verts[2] = z1 + inflate
            verts[3] = x1 + inflate; verts[4] = y1 + inflate; verts[5] = z1 + inflate
            verts[6] = x1 + inflate; verts[7] = y0 - inflate; verts[8] = z1 + inflate
            verts[9] = x0 - inflate; verts[10] = y0 - inflate; verts[11] = z1 + inflate
            texs[0] = uvMap["front"]!!.first.first; texs[1] = uvMap["front"]!!.second.second
            texs[2] = uvMap["front"]!!.second.first; texs[3] = uvMap["front"]!!.second.second
            texs[4] = uvMap["front"]!!.second.first; texs[5] = uvMap["front"]!!.first.second
            texs[6] = uvMap["front"]!!.first.first; texs[7] = uvMap["front"]!!.first.second
            norms[0] = 0f; norms[1] = 0f; norms[2] = 1f
            norms[3] = 0f; norms[4] = 0f; norms[5] = 1f
            norms[6] = 0f; norms[7] = 0f; norms[8] = 1f
            norms[9] = 0f; norms[10] = 0f; norms[11] = 1f

            // Back face (-Z)
            verts[12] = x1 + inflate; verts[13] = y1 + inflate; verts[14] = z0 - inflate
            verts[15] = x0 - inflate; verts[16] = y1 + inflate; verts[17] = z0 - inflate
            verts[18] = x0 - inflate; verts[19] = y0 - inflate; verts[20] = z0 - inflate
            verts[21] = x1 + inflate; verts[22] = y0 - inflate; verts[23] = z0 - inflate
            texs[8] = uvMap["back"]!!.second.first; texs[9] = uvMap["back"]!!.second.second
            texs[10] = uvMap["back"]!!.first.first; texs[11] = uvMap["back"]!!.second.second
            texs[12] = uvMap["back"]!!.first.first; texs[13] = uvMap["back"]!!.first.second
            texs[14] = uvMap["back"]!!.second.first; texs[15] = uvMap["back"]!!.first.second
            norms[12] = 0f; norms[13] = 0f; norms[14] = -1f
            norms[15] = 0f; norms[16] = 0f; norms[17] = -1f
            norms[18] = 0f; norms[19] = 0f; norms[20] = -1f
            norms[21] = 0f; norms[22] = 0f; norms[23] = -1f

            return ModelPart(verts, texs, norms, shortArrayOf(0, 1, 2, 0, 2, 3))
        }

        // Texture coordinates for each face of each part
        // Vanilla Minecraft skin layout (64x64)
        val uv = { u: Int, v: Int -> (u / 64f) to (v / 64f) }

        // Helper to create UV map entries with explicit parentheses
        fun e(k: String, u1: Int, v1: Int, u2: Int, v2: Int) = entry(k, entry(uv(u1, v1), uv(u2, v2)))

        val headUV = mapOf(
            e("front", 8, 8, 16, 16),
            e("back", 24, 8, 32, 16),
            e("right", 0, 8, 8, 16),
            e("left", 16, 8, 24, 16),
            e("top", 8, 0, 16, 8),
            e("bottom", 16, 0, 24, 8)
        )
        val hatUV = mapOf(
            e("front", 40, 8, 48, 16),
            e("back", 56, 8, 64, 16),
            e("right", 32, 8, 40, 16),
            e("left", 48, 8, 56, 16),
            e("top", 40, 0, 48, 8),
            e("bottom", 48, 0, 56, 8)
        )
        val bodyUV = mapOf(
            e("front", 20, 20, 28, 32),
            e("back", 32, 20, 40, 32),
            e("right", 16, 20, 20, 32),
            e("left", 28, 20, 32, 32),
            e("top", 20, 16, 28, 20),
            e("bottom", 28, 16, 36, 20)
        )
        val jacketUV = mapOf(
            e("front", 20, 36, 28, 48),
            e("back", 32, 36, 40, 48),
            e("right", 16, 36, 20, 48),
            e("left", 28, 36, 32, 48),
            e("top", 20, 32, 28, 36),
            e("bottom", 28, 32, 36, 36)
        )
        val armRUV = if (isSlim) mapOf(
            e("front", 44, 20, 47, 32),
            e("back", 51, 20, 54, 32),
            e("right", 40, 20, 43, 32),
            e("left", 47, 20, 50, 32),
            e("top", 44, 16, 47, 20),
            e("bottom", 47, 16, 50, 20)
        ) else mapOf(
            e("front", 44, 20, 48, 32),
            e("back", 52, 20, 56, 32),
            e("right", 40, 20, 44, 32),
            e("left", 48, 20, 52, 32),
            e("top", 44, 16, 48, 20),
            e("bottom", 48, 16, 52, 20)
        )
        val sleeveRUV = if (isSlim) mapOf(
            e("front", 44, 36, 47, 48),
            e("back", 51, 36, 54, 48),
            e("right", 40, 36, 43, 48),
            e("left", 47, 36, 50, 48),
            e("top", 44, 32, 47, 36),
            e("bottom", 47, 32, 50, 36)
        ) else mapOf(
            e("front", 44, 36, 48, 48),
            e("back", 52, 36, 56, 48),
            e("right", 40, 36, 44, 48),
            e("left", 48, 36, 52, 48),
            e("top", 44, 32, 48, 36),
            e("bottom", 48, 32, 52, 36)
        )
        val armLUV = if (isSlim) mapOf(
            e("front", 36, 52, 39, 64),
            e("back", 43, 52, 46, 64),
            e("right", 32, 52, 35, 64),
            e("left", 39, 52, 42, 64),
            e("top", 36, 48, 39, 52),
            e("bottom", 39, 48, 42, 52)
        ) else mapOf(
            e("front", 36, 52, 40, 64),
            e("back", 44, 52, 48, 64),
            e("right", 32, 52, 36, 64),
            e("left", 40, 52, 44, 64),
            e("top", 36, 48, 40, 52),
            e("bottom", 40, 48, 44, 52)
        )
        val sleeveLUV = if (isSlim) mapOf(
            e("front", 52, 52, 55, 64),
            e("back", 59, 52, 62, 64),
            e("right", 48, 52, 51, 64),
            e("left", 55, 52, 58, 64),
            e("top", 52, 48, 55, 52),
            e("bottom", 55, 48, 58, 52)
        ) else mapOf(
            e("front", 52, 52, 56, 64),
            e("back", 60, 52, 64, 64),
            e("right", 48, 52, 52, 64),
            e("left", 56, 52, 60, 64),
            e("top", 52, 48, 56, 52),
            e("bottom", 56, 48, 60, 52)
        )
        val legRUV = mapOf(
            e("front", 4, 20, 8, 32),
            e("back", 12, 20, 16, 32),
            e("right", 0, 20, 4, 32),
            e("left", 8, 20, 12, 32),
            e("top", 4, 16, 8, 20),
            e("bottom", 8, 16, 12, 20)
        )
        val pantsRUV = mapOf(
            e("front", 4, 36, 8, 48),
            e("back", 12, 36, 16, 48),
            e("right", 0, 36, 4, 48),
            e("left", 8, 36, 12, 48),
            e("top", 4, 32, 8, 36),
            e("bottom", 8, 32, 12, 36)
        )
        val legLUV = mapOf(
            e("front", 20, 52, 24, 64),
            e("back", 28, 52, 32, 64),
            e("right", 16, 52, 20, 64),
            e("left", 24, 52, 28, 64),
            e("top", 20, 48, 24, 52),
            e("bottom", 24, 48, 28, 52)
        )
        val pantsLUV = mapOf(
            e("front", 4, 52, 8, 64),
            e("back", 12, 52, 16, 64),
            e("right", 0, 52, 4, 64),
            e("left", 8, 52, 12, 64),
            e("top", 4, 48, 8, 52),
            e("bottom", 8, 48, 12, 52)
        )

        // Head
        parts.add(addBox(-4f, 24f, -4f, 4f, 32f, 4f, headUV))

        // Hat (overlay)
        if (overlayEnabled) {
            parts.add(addBox(-4f, 24.5f, -4f, 4f, 32f, 4f, hatUV, 0.5f))
        }

        // Body
        parts.add(addBox(-4f, 12f, -2f, 4f, 24f, 2f, bodyUV))

        // Jacket (overlay)
        if (overlayEnabled) {
            parts.add(addBox(-4f, 12f, -2f, 4f, 24f, 2f, jacketUV, 0.25f))
        }

        // Right arm
        val armRX0 = if (isSlim) -7f else -8f
        parts.add(addBox(armRX0, 12f, -2f, -4f, 24f, 2f, armRUV))

        // Right sleeve (overlay)
        if (overlayEnabled) {
            parts.add(addBox(armRX0, 12f, -2f, -4f, 24f, 2f, sleeveRUV, 0.25f))
        }

        // Left arm
        val armLX1 = if (isSlim) 7f else 8f
        parts.add(addBox(4f, 12f, -2f, armLX1, 24f, 2f, armLUV))

        // Left sleeve (overlay)
        if (overlayEnabled) {
            parts.add(addBox(4f, 12f, -2f, armLX1, 24f, 2f, sleeveLUV, 0.25f))
        }

        // Right leg
        parts.add(addBox(-4f, 0f, -2f, 0f, 12f, 2f, legRUV))

        // Right pants (overlay)
        if (overlayEnabled) {
            parts.add(addBox(-4f, 0f, -2f, 0f, 12f, 2f, pantsRUV, 0.25f))
        }

        // Left leg
        parts.add(addBox(0f, 0f, -2f, 4f, 12f, 2f, legLUV))

        // Left pants (overlay)
        if (overlayEnabled) {
            parts.add(addBox(0f, 0f, -2f, 4f, 12f, 2f, pantsLUV, 0.25f))
        }

        return parts
    }

    private const val VERTEX_SHADER_SOURCE = """
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

            // Simple directional lighting
            vec3 normal = normalize(aNormal);
            float diff = max(dot(normal, normalize(uLightDir)), 0.0);
            vLighting = 0.6 + 0.4 * diff;
        }
    """.trimIndent()

    private const val FRAGMENT_SHADER_SOURCE = """
        precision mediump float;

        uniform sampler2D uTexture;
        uniform sampler2D uOverlayTexture;
        uniform int uUseOverlay;
        uniform float uAlpha;
        uniform float uOverlayAlpha;

        varying vec2 vTexCoord;
        varying float vLighting;

        void main() {
            vec4 base = texture2D(uTexture, vTexCoord);
            base.rgb *= vLighting;
            base.a *= uAlpha;

            if (uUseOverlay == 1) {
                vec4 overlay = texture2D(uOverlayTexture, vTexCoord);
                overlay.rgb *= vLighting;
                overlay.a *= uOverlayAlpha;

                // Alpha blend overlay over base
                float alpha = base.a + overlay.a * (1.0 - base.a);
                vec3 color = (base.rgb * base.a + overlay.rgb * overlay.a * (1.0 - base.a)) / max(alpha, 0.001);
                gl_FragColor = vec4(color, alpha);
            } else {
                gl_FragColor = base;
            }

            // Discard transparent pixels
            if (gl_FragColor.a < 0.01) discard;
        }
    """.trimIndent()
}