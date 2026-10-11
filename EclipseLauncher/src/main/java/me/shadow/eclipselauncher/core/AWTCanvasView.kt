package me.shadow.eclipselauncher.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.text.TextPaint
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runInterruptible

import me.shadow.eclipselauncher.core.utils.JREUtils

import java.util.LinkedList

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class AWTCanvasView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) :
    TextureView(ctx, attrs), TextureView.SurfaceTextureListener {

    companion object {
        // Public static final int in the old class: Java callers read these as plain fields
        @JvmField
        val AWT_CANVAS_WIDTH: Int = (Tools.currentDisplayMetrics.widthPixels * 0.8).toInt()
        @JvmField
        val AWT_CANVAS_HEIGHT: Int = (Tools.currentDisplayMetrics.heightPixels * 0.8).toInt()
        private const val MAX_SIZE = 100
        private const val NANOS = 1000000000.0
    }

    private var mIsDestroyed = false
    private val mFpsPaint: TextPaint

    // Temporary count fps https://stackoverflow.com/a/13729241
    private val mTimes = LinkedList<Long>().apply { add(System.nanoTime()) }

    // The dedicated "AndroidAWTRenderer" thread of the old code, as a single-thread dispatcher,
    // created lazily on the first frame loop - like the old new Thread(this, "AndroidAWTRenderer")
    // only ever started from onSurfaceTextureAvailable. It is intentionally never closed, just
    // like the old renderer thread was never joined: the loop exits on its own when the surface
    // is destroyed.
    private val rendererScope: CoroutineScope by lazy(LazyThreadSafetyMode.NONE) {
        CoroutineScope(SupervisorJob() + newSingleThreadContext("AndroidAWTRenderer") + CoroutineName("AWTCanvasView"))
    }

    init {
        mFpsPaint = TextPaint()
        mFpsPaint.setColor(Color.WHITE)
        mFpsPaint.setTextSize(24f)

        surfaceTextureListener = this

        post { refreshSize() }
    }

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, w: Int, h: Int) {
        getSurfaceTexture()!!.setDefaultBufferSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT)
        mIsDestroyed = false
        rendererScope.launch {
            runInterruptible { renderFrame() }
        }
    }

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        mIsDestroyed = true
        return true
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, w: Int, h: Int) {
        getSurfaceTexture()!!.setDefaultBufferSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT)
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
        getSurfaceTexture()!!.setDefaultBufferSize(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT)
    }

    /** The frame render loop, formerly run on the "AndroidAWTRenderer" thread */
    private fun renderFrame() {
        val surface = Surface(getSurfaceTexture())
        val rgbArrayBitmap = Bitmap.createBitmap(AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val paint = Paint()
        try {
            while (!mIsDestroyed && surface.isValid) {
                // One fresh canvas per frame, exactly like the old reassignment of the loop-scoped
                // canvas variable: lockCanvas() returns a platform-typed (non-null) canvas.
                val canvas = surface.lockCanvas(null)
                canvas.drawRGB(0, 0, 0)
                val rgbArray = JREUtils.renderAWTScreenFrame(/* canvas, mWidth, mHeight */)
                val mDrawing = rgbArray != null
                if (rgbArray != null) {
                    canvas.save()
                    rgbArrayBitmap.setPixels(rgbArray, 0, AWT_CANVAS_WIDTH, 0, 0, AWT_CANVAS_WIDTH, AWT_CANVAS_HEIGHT)
                    canvas.drawBitmap(rgbArrayBitmap, 0f, 0f, paint)
                    canvas.restore()
                }
                canvas.drawText("FPS: " + (Math.round(fps() * 10) / 10) + ", drawing=" + mDrawing, 20f, 20f, mFpsPaint)
                surface.unlockCanvasAndPost(canvas)
            }
        } catch (throwable: Throwable) {
            Tools.showError(getContext(), throwable)
        }
        rgbArrayBitmap.recycle()
        surface.release()
    }

    /** Calculates and returns frames per second */
    private fun fps(): Double {
        val lastTime = System.nanoTime()
        val difference = (lastTime - mTimes.first) / NANOS
        mTimes.addLast(lastTime)
        val size = mTimes.size
        if (size > MAX_SIZE) {
            mTimes.removeFirst()
        }
        return if (difference > 0) mTimes.size / difference else 0.0
    }

    /** Make the view fit the proper aspect ratio of the surface */
    private fun refreshSize() {
        val layoutParams = layoutParams

        if (height < width) {
            layoutParams.width = AWT_CANVAS_WIDTH * height / AWT_CANVAS_HEIGHT
        } else {
            layoutParams.height = AWT_CANVAS_HEIGHT * width / AWT_CANVAS_WIDTH
        }

        setLayoutParams(layoutParams)
    }
}
