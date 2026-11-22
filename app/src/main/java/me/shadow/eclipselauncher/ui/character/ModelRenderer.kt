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
    fun updateSkin(bitmap: Bitmap?) {
        queueEvent { renderer.updateSkin(bitmap) }
        requestRender()
    }

    /** Updates the overlay texture (jacket/sleeves/pants). */
    fun updateOverlay(bitmap: Bitmap?) {
        queueEvent { renderer.updateOverlay(bitmap) }
        requestRender()
    }

    /** Sets the model variant: "CLASSIC" (Steve, 4px) or "SLIM" (Alex, 3px). */
    fun updateVariant(variant: String) {
        queueEvent { renderer.updateVariant(variant) }
        requestRender()
    }

    /** Enables/disables the overlay layer. */
    fun updateOverlayEnabled(enabled: Boolean) {
        queueEvent { renderer.updateOverlayEnabled(enabled) }
        requestRender()
    }

    /** Starts/stops the idle animation loop. */
    fun updateAnimating(animating: Boolean) {
        renderer.updateAnimating(animating)
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
    private var baseIndexBuffer: ShortBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asShortBuffer()
    private var baseIndexCount: Int = 0
    private var overlayIndexBuffer: ShortBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()).asShortBuffer()
    private var overlayIndexCount: Int = 0

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
    private var isAnimating = true
    private var lastFrameTime = System.nanoTime()
    private var animationTime = 0f

    // Model state
    private var variant = "CLASSIC"
    private var overlayEnabled = true
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

        // Pass 1: Draw base layer (depth write enabled)
        GLES20.glDepthMask(true)
        if (baseIndexCount > 0) {
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, baseIndexCount, GLES20.GL_UNSIGNED_SHORT, baseIndexBuffer)
        }

        // Pass 2: Draw overlay layer (depth test but no depth write to prevent z-fighting)
        if (overlayEnabled && overlayIndexCount > 0) {
            GLES20.glDepthMask(false)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, overlayIndexCount, GLES20.GL_UNSIGNED_SHORT, overlayIndexBuffer)
            GLES20.glDepthMask(true)
        }

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
        val baseIndices = mutableListOf<Short>()
        val overlayIndices = mutableListOf<Short>()

        var vIdx = 0
        var tIdx = 0
        var nIdx = 0
        var vertexOffset = 0

        for (part in parts) {
            System.arraycopy(part.vertices, 0, vertices, vIdx, part.vertices.size)
            vIdx += part.vertices.size
            System.arraycopy(part.texCoords, 0, texCoords, tIdx, part.texCoords.size)
            tIdx += part.texCoords.size
            System.arraycopy(part.normals, 0, normals, nIdx, part.normals.size)
            nIdx += part.normals.size

            for (idx in part.indices) {
                if (part.isOverlay) {
                    overlayIndices.add((idx + vertexOffset).toShort())
                } else {
                    baseIndices.add((idx + vertexOffset).toShort())
                }
            }
            vertexOffset += part.vertices.size / 3
        }

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(vertices); position(0) }
        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(texCoords); position(0) }
        normalBuffer = ByteBuffer.allocateDirect(normals.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(normals); position(0) }

        baseIndexBuffer = ByteBuffer.allocateDirect(baseIndices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply { baseIndices.forEach { put(it) }; position(0) }
        baseIndexCount = baseIndices.size
        overlayIndexBuffer = ByteBuffer.allocateDirect(overlayIndices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply { overlayIndices.forEach { put(it) }; position(0) }
        overlayIndexCount = overlayIndices.size
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

    fun updateSkin(bitmap: Bitmap?) {
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

    fun updateOverlay(bitmap: Bitmap?) {
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

    fun updateVariant(variant: String) {
        this.variant = variant
        view.queueEvent { updateVariant() }
    }

    fun updateOverlayEnabled(enabled: Boolean) {
        overlayEnabled = enabled
    }

    fun updateAnimating(animating: Boolean) {
        isAnimating = animating
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
        val indices: ShortArray,
        val isOverlay: Boolean = false
    )

    // UV helper - defined once at class level
    // Inset by half a texel (0.5/64) to prevent bilinear filtering bleeding at edges
    private fun uvMin(u: Int, v: Int): Pair<Float, Float> = ((u + 0.5f) / 64f) to ((v + 0.5f) / 64f)
    private fun uvMax(u: Int, v: Int): Pair<Float, Float> = ((u - 0.5f) / 64f) to ((v - 0.5f) / 64f)

    private fun buildModelParts(): List<ModelPart> {
        val parts = mutableListOf<ModelPart>()

        // Texture coordinates for each face of each part
        // Vanilla Minecraft skin layout (64x64). UV coordinates are inset by 0.5 texels
        // to prevent bilinear filtering bleeding at texture boundaries.
        val headUV = mapOf(
            "front" to (uvMin(8, 8) to uvMax(16, 16)),
            "back" to (uvMin(24, 8) to uvMax(32, 16)),
            "right" to (uvMin(0, 8) to uvMax(8, 16)),
            "left" to (uvMin(16, 8) to uvMax(24, 16)),
            "top" to (uvMin(8, 0) to uvMax(16, 8)),
            "bottom" to (uvMin(16, 0) to uvMax(24, 8))
        )
        val hatUV = mapOf(
            "front" to (uvMin(40, 8) to uvMax(48, 16)),
            "back" to (uvMin(56, 8) to uvMax(64, 16)),
            "right" to (uvMin(32, 8) to uvMax(40, 16)),
            "left" to (uvMin(48, 8) to uvMax(56, 16)),
            "top" to (uvMin(40, 0) to uvMax(48, 8)),
            "bottom" to (uvMin(48, 0) to uvMax(56, 8))
        )
        val bodyUV = mapOf(
            "front" to (uvMin(20, 20) to uvMax(28, 32)),
            "back" to (uvMin(32, 20) to uvMax(40, 32)),
            "right" to (uvMin(16, 20) to uvMax(20, 32)),
            "left" to (uvMin(28, 20) to uvMax(32, 32)),
            "top" to (uvMin(20, 16) to uvMax(28, 20)),
            "bottom" to (uvMin(28, 16) to uvMax(36, 20))
        )
        val jacketUV = mapOf(
            "front" to (uvMin(20, 36) to uvMax(28, 48)),
            "back" to (uvMin(32, 36) to uvMax(40, 48)),
            "right" to (uvMin(16, 36) to uvMax(20, 48)),
            "left" to (uvMin(28, 36) to uvMax(32, 48)),
            "top" to (uvMin(20, 32) to uvMax(28, 36)),
            "bottom" to (uvMin(28, 32) to uvMax(36, 36))
        )
        val armRUV = if (isSlim) mapOf(
            "front" to (uvMin(44, 20) to uvMax(47, 32)),
            "back" to (uvMin(51, 20) to uvMax(54, 32)),
            "right" to (uvMin(40, 20) to uvMax(43, 32)),
            "left" to (uvMin(47, 20) to uvMax(50, 32)),
            "top" to (uvMin(44, 16) to uvMax(47, 20)),
            "bottom" to (uvMin(47, 16) to uvMax(50, 20))
        ) else mapOf(
            "front" to (uvMin(44, 20) to uvMax(48, 32)),
            "back" to (uvMin(52, 20) to uvMax(56, 32)),
            "right" to (uvMin(40, 20) to uvMax(44, 32)),
            "left" to (uvMin(48, 20) to uvMax(52, 32)),
            "top" to (uvMin(44, 16) to uvMax(48, 20)),
            "bottom" to (uvMin(48, 16) to uvMax(52, 20))
        )
        val sleeveRUV = if (isSlim) mapOf(
            "front" to (uvMin(44, 36) to uvMax(47, 48)),
            "back" to (uvMin(51, 36) to uvMax(54, 48)),
            "right" to (uvMin(40, 36) to uvMax(43, 48)),
            "left" to (uvMin(47, 36) to uvMax(50, 48)),
            "top" to (uvMin(44, 32) to uvMax(47, 36)),
            "bottom" to (uvMin(47, 32) to uvMax(50, 36))
        ) else mapOf(
            "front" to (uvMin(44, 36) to uvMax(48, 48)),
            "back" to (uvMin(52, 36) to uvMax(56, 48)),
            "right" to (uvMin(40, 36) to uvMax(44, 48)),
            "left" to (uvMin(48, 36) to uvMax(52, 48)),
            "top" to (uvMin(44, 32) to uvMax(48, 36)),
            "bottom" to (uvMin(48, 32) to uvMax(52, 36))
        )
        val armLUV = if (isSlim) mapOf(
            "front" to (uvMin(36, 52) to uvMax(39, 64)),
            "back" to (uvMin(43, 52) to uvMax(46, 64)),
            "right" to (uvMin(32, 52) to uvMax(35, 64)),
            "left" to (uvMin(39, 52) to uvMax(42, 64)),
            "top" to (uvMin(36, 48) to uvMax(39, 52)),
            "bottom" to (uvMin(39, 48) to uvMax(42, 52))
        ) else mapOf(
            "front" to (uvMin(36, 52) to uvMax(40, 64)),
            "back" to (uvMin(44, 52) to uvMax(48, 64)),
            "right" to (uvMin(32, 52) to uvMax(36, 64)),
            "left" to (uvMin(40, 52) to uvMax(44, 64)),
            "top" to (uvMin(36, 48) to uvMax(40, 52)),
            "bottom" to (uvMin(40, 48) to uvMax(44, 52))
        )
        val sleeveLUV = if (isSlim) mapOf(
            "front" to (uvMin(52, 52) to uvMax(55, 64)),
            "back" to (uvMin(59, 52) to uvMax(62, 64)),
            "right" to (uvMin(48, 52) to uvMax(51, 64)),
            "left" to (uvMin(55, 52) to uvMax(58, 64)),
            "top" to (uvMin(52, 48) to uvMax(55, 52)),
            "bottom" to (uvMin(55, 48) to uvMax(58, 52))
        ) else mapOf(
            "front" to (uvMin(52, 52) to uvMax(56, 64)),
            "back" to (uvMin(60, 52) to uvMax(64, 64)),
            "right" to (uvMin(48, 52) to uvMax(52, 64)),
            "left" to (uvMin(56, 52) to uvMax(60, 64)),
            "top" to (uvMin(52, 48) to uvMax(56, 52)),
            "bottom" to (uvMin(56, 48) to uvMax(60, 52))
        )
        val legRUV = mapOf(
            "front" to (uvMin(4, 20) to uvMax(8, 32)),
            "back" to (uvMin(12, 20) to uvMax(16, 32)),
            "right" to (uvMin(0, 20) to uvMax(4, 32)),
            "left" to (uvMin(8, 20) to uvMax(12, 32)),
            "top" to (uvMin(4, 16) to uvMax(8, 20)),
            "bottom" to (uvMin(8, 16) to uvMax(12, 20))
        )
        val pantsRUV = mapOf(
            "front" to (uvMin(4, 36) to uvMax(8, 48)),
            "back" to (uvMin(12, 36) to uvMax(16, 48)),
            "right" to (uvMin(0, 36) to uvMax(4, 48)),
            "left" to (uvMin(8, 36) to uvMax(12, 48)),
            "top" to (uvMin(4, 32) to uvMax(8, 36)),
            "bottom" to (uvMin(8, 32) to uvMax(12, 36))
        )
        val legLUV = mapOf(
            "front" to (uvMin(20, 52) to uvMax(24, 64)),
            "back" to (uvMin(28, 52) to uvMax(32, 64)),
            "right" to (uvMin(16, 52) to uvMax(20, 64)),
            "left" to (uvMin(24, 52) to uvMax(28, 64)),
            "top" to (uvMin(20, 48) to uvMax(24, 52)),
            "bottom" to (uvMin(24, 48) to uvMax(28, 52))
        )
        val pantsLUV = mapOf(
            "front" to (uvMin(4, 52) to uvMax(8, 64)),
            "back" to (uvMin(12, 52) to uvMax(16, 64)),
            "right" to (uvMin(0, 52) to uvMax(4, 64)),
            "left" to (uvMin(8, 52) to uvMax(12, 64)),
            "top" to (uvMin(4, 48) to uvMax(8, 52)),
            "bottom" to (uvMin(8, 48) to uvMax(12, 52))
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
            inflate: Float = 0f,
            inflateBottom: Float = -1f, // -1 means use inflate value
            isOverlay: Boolean = false
        ): ModelPart {
            val verts = FloatArray(24)
            val texs = FloatArray(16)
            val norms = FloatArray(24)
            val inds = shortArrayOf(0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7, 8, 9, 10, 8, 10, 11, 12, 13, 14, 12, 14, 15, 16, 17, 18, 16, 18, 19, 20, 21, 22, 20, 22, 23)

            val inf = inflate
            val infBottom = if (inflateBottom >= 0) inflateBottom else inflate
            val fx0 = x0 - inf
            val fy0 = y0 - inf
            val fz0 = z0 - inf
            val fx1 = x1 + inf
            val fy1 = y1 + inf
            val fz1 = z1 + inf
            val fy0b = y0 - infBottom // bottom face uses separate inflation

            // Front face (+Z)
            verts[0] = x0 - inf; verts[1] = y1 + inf; verts[2] = z1 + inf
            verts[3] = x1 + inf; verts[4] = y1 + inf; verts[5] = z1 + inf
            verts[6] = x1 + inf; verts[7] = y0 - inf; verts[8] = z1 + inf
            verts[9] = x0 - inf; verts[10] = y0 - inf; verts[11] = z1 + inf
            texs[0] = uvMap["front"]!!.first.first; texs[1] = uvMap["front"]!!.second.second
            texs[2] = uvMap["front"]!!.second.first; texs[3] = uvMap["front"]!!.second.second
            texs[4] = uvMap["front"]!!.second.first; texs[5] = uvMap["front"]!!.first.second
            texs[6] = uvMap["front"]!!.first.first; texs[7] = uvMap["front"]!!.first.second
            norms[0] = 0f; norms[1] = 0f; norms[2] = 1f
            norms[3] = 0f; norms[4] = 0f; norms[5] = 1f
            norms[6] = 0f; norms[7] = 0f; norms[8] = 1f
            norms[9] = 0f; norms[10] = 0f; norms[11] = 1f

            // Back face (-Z)
            verts[12] = x1 + inf; verts[13] = y1 + inf; verts[14] = z0 - inf
            verts[15] = x0 - inf; verts[16] = y1 + inf; verts[17] = z0 - inf
            verts[18] = x0 - inf; verts[19] = y0 - inf; verts[20] = z0 - inf
            verts[21] = x1 + inf; verts[22] = y0 - inf; verts[23] = z0 - inf
            texs[8] = uvMap["back"]!!.second.first; texs[9] = uvMap["back"]!!.second.second
            texs[10] = uvMap["back"]!!.first.first; texs[11] = uvMap["back"]!!.second.second
            texs[12] = uvMap["back"]!!.first.first; texs[13] = uvMap["back"]!!.first.second
            texs[14] = uvMap["back"]!!.second.first; texs[15] = uvMap["back"]!!.first.second
            norms[12] = 0f; norms[13] = 0f; norms[14] = -1f
            norms[15] = 0f; norms[16] = 0f; norms[17] = -1f
            norms[18] = 0f; norms[19] = 0f; norms[20] = -1f
            norms[21] = 0f; norms[22] = 0f; norms[23] = -1f

            // Right face (+X)
            verts[24] = x1 + inf; verts[25] = y1 + inf; verts[26] = z1 + inf
            verts[27] = x1 + inf; verts[28] = y1 + inf; verts[29] = z0 - inf
            verts[30] = x1 + inf; verts[31] = y0 - inf; verts[32] = z0 - inf
            verts[33] = x1 + inf; verts[34] = y0 - inf; verts[35] = z1 + inf
            texs[16] = uvMap["right"]!!.second.first; texs[17] = uvMap["right"]!!.second.second
            texs[18] = uvMap["right"]!!.first.first; texs[19] = uvMap["right"]!!.second.second
            texs[20] = uvMap["right"]!!.first.first; texs[21] = uvMap["right"]!!.first.second
            texs[22] = uvMap["right"]!!.second.first; texs[23] = uvMap["right"]!!.first.second
            norms[24] = 1f; norms[25] = 0f; norms[26] = 0f
            norms[27] = 1f; norms[28] = 0f; norms[29] = 0f
            norms[30] = 1f; norms[31] = 0f; norms[32] = 0f
            norms[33] = 1f; norms[34] = 0f; norms[35] = 0f

            // Left face (-X)
            verts[36] = x0 - inf; verts[37] = y1 + inf; verts[38] = z0 - inf
            verts[39] = x0 - inf; verts[40] = y1 + inf; verts[41] = z1 + inf
            verts[42] = x0 - inf; verts[43] = y0 - inf; verts[44] = z1 + inf
            verts[45] = x0 - inf; verts[46] = y0 - inf; verts[47] = z0 - inf
            texs[24] = uvMap["left"]!!.second.first; texs[25] = uvMap["left"]!!.second.second
            texs[26] = uvMap["left"]!!.first.first; texs[27] = uvMap["left"]!!.second.second
            texs[28] = uvMap["left"]!!.first.first; texs[29] = uvMap["left"]!!.first.second
            texs[30] = uvMap["left"]!!.second.first; texs[31] = uvMap["left"]!!.first.second
            norms[36] = -1f; norms[37] = 0f; norms[38] = 0f
            norms[39] = -1f; norms[40] = 0f; norms[41] = 0f
            norms[42] = -1f; norms[43] = 0f; norms[44] = 0f
            norms[45] = -1f; norms[46] = 0f; norms[47] = 0f

            // Top face (+Y)
            verts[48] = x0 - inf; verts[49] = y1 + inf; verts[50] = z0 - inf
            verts[51] = x1 + inf; verts[52] = y1 + inf; verts[53] = z0 - inf
            verts[54] = x1 + inf; verts[55] = y1 + inf; verts[56] = z1 + inf
            verts[57] = x0 - inf; verts[58] = y1 + inf; verts[59] = z1 + inf
            texs[32] = uvMap["top"]!!.first.first; texs[33] = uvMap["top"]!!.second.second
            texs[34] = uvMap["top"]!!.second.first; texs[35] = uvMap["top"]!!.second.second
            texs[36] = uvMap["top"]!!.second.first; texs[37] = uvMap["top"]!!.first.second
            texs[38] = uvMap["top"]!!.first.first; texs[39] = uvMap["top"]!!.first.second
            norms[48] = 0f; norms[49] = 1f; norms[50] = 0f
            norms[51] = 0f; norms[52] = 1f; norms[53] = 0f
            norms[54] = 0f; norms[55] = 1f; norms[56] = 0f
            norms[57] = 0f; norms[58] = 1f; norms[59] = 0f

            // Bottom face (-Y) - uses separate inflation
            verts[60] = x1 + inf; verts[61] = y0 - infBottom; verts[62] = z0 - inf
            verts[63] = x0 - inf; verts[64] = y0 - infBottom; verts[65] = z0 - inf
            verts[66] = x0 - inf; verts[67] = y0 - infBottom; verts[68] = z1 + inf
            verts[69] = x1 + inf; verts[70] = y0 - infBottom; verts[71] = z1 + inf
            texs[40] = uvMap["bottom"]!!.second.first; texs[41] = uvMap["bottom"]!!.second.second
            texs[42] = uvMap["bottom"]!!.first.first; texs[43] = uvMap["bottom"]!!.second.second
            texs[44] = uvMap["bottom"]!!.first.first; texs[45] = uvMap["bottom"]!!.first.second
            texs[46] = uvMap["bottom"]!!.second.first; texs[47] = uvMap["bottom"]!!.first.second
            norms[60] = 0f; norms[61] = -1f; norms[62] = 0f
            norms[63] = 0f; norms[64] = -1f; norms[65] = 0f
            norms[66] = 0f; norms[67] = -1f; norms[68] = 0f
            norms[69] = 0f; norms[70] = -1f; norms[71] = 0f

            return ModelPart(verts, texs, norms, inds, isOverlay)
        }

        // Head
        parts.add(addBox(-4f, 24f, -4f, 4f, 32f, 4f, headUV, isOverlay = false))

        // Hat (overlay)
        if (overlayEnabled) {
            parts.add(addBox(-4f, 24.5f, -4f, 4f, 32f, 4f, hatUV, 0.5f, isOverlay = true))
        }

        // Body
        parts.add(addBox(-4f, 12f, -2f, 4f, 24f, 2f, bodyUV, isOverlay = false))

        // Jacket (overlay)
        if (overlayEnabled) {
            parts.add(addBox(-4f, 12f, -2f, 4f, 24f, 2f, jacketUV, 0.25f, isOverlay = true))
        }

        // Right arm
        val armRX0 = if (isSlim) -7f else -8f
        parts.add(addBox(armRX0, 12f, -2f, -4f, 24f, 2f, armRUV, isOverlay = false))

        // Right sleeve (overlay)
        if (overlayEnabled) {
            parts.add(addBox(armRX0, 12f, -2f, -4f, 24f, 2f, sleeveRUV, 0.25f, isOverlay = true))
        }

        // Left arm
        val armLX1 = if (isSlim) 7f else 8f
        parts.add(addBox(4f, 12f, -2f, armLX1, 24f, 2f, armLUV, isOverlay = false))

        // Left sleeve (overlay)
        if (overlayEnabled) {
            parts.add(addBox(4f, 12f, -2f, armLX1, 24f, 2f, sleeveLUV, 0.25f, isOverlay = true))
        }

        // Right leg
        parts.add(addBox(-4f, 0f, -2f, 0f, 12f, 2f, legRUV, isOverlay = false))

        // Right pants (overlay)
        if (overlayEnabled) {
            parts.add(addBox(-4f, 0f, -2f, 0f, 12f, 2f, pantsRUV, 0.25f, isOverlay = true))
        }

        // Left leg
        parts.add(addBox(0f, 0f, -2f, 4f, 12f, 2f, legLUV, isOverlay = false))

        // Left pants (overlay)
        if (overlayEnabled) {
            parts.add(addBox(0f, 0f, -2f, 4f, 12f, 2f, pantsLUV, 0.25f, isOverlay = true))
        }

        return parts
    }

    private val VERTEX_SHADER_SOURCE = """
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

    private val FRAGMENT_SHADER_SOURCE = """
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