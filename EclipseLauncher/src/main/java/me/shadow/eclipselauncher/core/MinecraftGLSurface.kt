package me.shadow.eclipselauncher.core

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

import me.shadow.eclipselauncher.core.customcontrols.ControlLayout
import me.shadow.eclipselauncher.core.customcontrols.gamepad.DefaultDataProvider
import me.shadow.eclipselauncher.core.customcontrols.gamepad.Gamepad
import me.shadow.eclipselauncher.core.customcontrols.mouse.AbstractTouchpad
import me.shadow.eclipselauncher.core.customcontrols.mouse.AndroidPointerCapture
import me.shadow.eclipselauncher.core.customcontrols.mouse.InGUIEventProcessor
import me.shadow.eclipselauncher.core.customcontrols.mouse.InGameEventProcessor
import me.shadow.eclipselauncher.core.customcontrols.mouse.TouchEventProcessor
import me.shadow.eclipselauncher.core.utils.JREUtils
import me.shadow.eclipselauncher.event.single.RefreshHotbarEvent
import me.shadow.eclipselauncher.feature.MCOptions
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.setting.AllStaticSettings
import me.shadow.eclipselauncher.ui.activity.BaseActivity

import org.greenrobot.eventbus.EventBus
import org.lwjgl.glfw.CallbackBridge

import java.util.Locale

import fr.spse.gamepad_remapper.RemapperManager
import fr.spse.gamepad_remapper.RemapperView

/**
 * Class dealing with showing minecraft surface and taking inputs to dispatch them to minecraft
 */
class MinecraftGLSurface @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null) :
    View(context, attributeSet), GrabListener {

    /* Gamepad object for gamepad inputs, instantiated on need */
    private var mGamepad: Gamepad? = null

    /* The RemapperView.Builder object allows you to set which buttons to remap */
    private val mInputManager = RemapperManager(context, RemapperView.Builder(null)
        .remapA(true)
        .remapB(true)
        .remapX(true)
        .remapY(true)

        .remapLeftJoystick(true)
        .remapRightJoystick(true)
        .remapStart(true)
        .remapSelect(true)
        .remapLeftShoulder(true)
        .remapRightShoulder(true)
        .remapLeftTrigger(true)
        .remapRightTrigger(true)
        .remapDpad(true))

    /* Sensitivity, adjusted according to screen size */
    private val mSensitivityFactor =
        (1.4 * (1080f / Tools.getDisplayMetrics(context as BaseActivity).heightPixels))

    /*
     * Surface ready signal, used by the activity to launch minecraft. It replaces the old
     * mSurfaceReadyListener + monitor: the "JVM Main thread" coroutine below parks on it until
     * setSurfaceReadyListener() completes it, exactly like wait()/notifyAll() did before.
     */
    private val mSurfaceReadySignal = CompletableDeferred<SurfaceReadyListener>()

    /* View holding the surface, either a SurfaceView or a TextureView */
    private var mSurface: View? = null

    private val mIngameProcessor = InGameEventProcessor(mSensitivityFactor)
    private val mInGUIProcessor = InGUIEventProcessor()
    private var mCurrentTouchProcessor: TouchEventProcessor = mInGUIProcessor
    private var mPointerCapture: AndroidPointerCapture? = null
    private var mLastGrabState = false

    private var mOnRenderingStartedListener: OnRenderingStartedListener? = null
    private var mIsRenderingStarted = false

    private fun setUpPointerCapture(touchpad: AbstractTouchpad?) {
        mPointerCapture?.detach()
        mPointerCapture = AndroidPointerCapture(touchpad, this)
    }

    /** Initialize the view and all its settings
     * @param isAlreadyRunning set to true to tell the view that the game is already running
     *                         (only updates the window without calling the start listener)
     * @param touchpad the optional cursor-emulating touchpad, used for touch event processing
     *                 when the cursor is not grabbed
     */
    fun start(isAlreadyRunning: Boolean, touchpad: AbstractTouchpad?) {
        setUpPointerCapture(touchpad)
        mInGUIProcessor.setAbstractTouchpad(touchpad)
        if (AllSettings.alternateSurface.getValue()) {
            val surfaceView = SurfaceView(context)
            mSurface = surfaceView

            surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                private var isCalled = isAlreadyRunning

                override fun surfaceCreated(holder: SurfaceHolder) {
                    if (isCalled) {
                        JREUtils.setupBridgeWindow(surfaceView.holder.surface)
                        return
                    }
                    isCalled = true

                    realStart(surfaceView.holder.surface)
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    refreshSize()
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {}
            })

            (parent as ViewGroup).addView(surfaceView)
        } else {
            val textureView = TextureView(context)
            textureView.isOpaque = true
            textureView.alpha = 1.0f
            mSurface = textureView

            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                private var isCalled = isAlreadyRunning

                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    val tSurface = Surface(surface)
                    if (isCalled) {
                        JREUtils.setupBridgeWindow(tSurface)
                        return
                    }
                    isCalled = true

                    realStart(tSurface)
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                    refreshSize()
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                    if (!mIsRenderingStarted) {
                        mIsRenderingStarted = true
                        //When actually rendering the frame, call this listener to turn off the launcher background image, preventing translucency issues on some devices
                        mOnRenderingStartedListener?.isStarted()
                    }
                }
            }

            (parent as ViewGroup).addView(textureView)
        }
    }

    /**
     * The touch event for both grabbed an non-grabbed mouse state on the touch screen
     * Does not cover the virtual mouse touchpad
     */
    @Suppress("accessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        // Kinda need to send this back to the layout
        if ((parent as ControlLayout).modifiable) return false

        // Looking for a mouse to handle, won't have an effect if no mouse exists.
        for (i in 0 until e.pointerCount) {
            val toolType = e.getToolType(i)
            if (toolType == MotionEvent.TOOL_TYPE_MOUSE) {
                val pointerCapture = mPointerCapture
                if (pointerCapture != null) {
                    pointerCapture.handleAutomaticCapture()
                    return true
                }
            } else if (toolType != MotionEvent.TOOL_TYPE_STYLUS) continue

            // Mouse found
            if (CallbackBridge.isGrabbing()) return false
            CallbackBridge.sendCursorPos(e.getX(i) * AllStaticSettings.scaleFactor, e.getY(i) * AllStaticSettings.scaleFactor)
            return true //mouse event handled successfully
        }
        @Suppress("SENSELESS_COMPARISON")
        if (mIngameProcessor == null || mInGUIProcessor == null) return true
        return mCurrentTouchProcessor.processTouchEvent(e)
    }

    private fun createGamepad(contextView: View, inputDevice: InputDevice) {
        mGamepad = Gamepad(contextView, inputDevice, DefaultDataProvider.INSTANCE, true)
    }

    /**
     * The event for mouse/joystick movements
     */
    @SuppressLint("NewApi")
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        var mouseCursorIndex = -1

        if (Gamepad.isGamepadEvent(event)) {
            if (mGamepad == null) createGamepad(this, event.device)

            mInputManager.handleMotionEventInput(context, event, mGamepad)
            return true
        }

        for (i in 0 until event.pointerCount) {
            if (event.getToolType(i) != MotionEvent.TOOL_TYPE_MOUSE && event.getToolType(i) != MotionEvent.TOOL_TYPE_STYLUS) continue
            // Mouse found
            mouseCursorIndex = i
            break
        }
        if (mouseCursorIndex == -1) return false // we cant consoom that, theres no mice!

        // Make sure we grabbed the mouse if necessary
        updateGrabState(CallbackBridge.isGrabbing())

        return when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_MOVE -> {
                CallbackBridge.mouseX = event.getX(mouseCursorIndex) * AllStaticSettings.scaleFactor
                CallbackBridge.mouseY = event.getY(mouseCursorIndex) * AllStaticSettings.scaleFactor
                CallbackBridge.sendCursorPos(CallbackBridge.mouseX, CallbackBridge.mouseY)
                true
            }
            MotionEvent.ACTION_SCROLL -> {
                CallbackBridge.sendScroll(event.getAxisValue(MotionEvent.AXIS_HSCROLL).toDouble(), event.getAxisValue(MotionEvent.AXIS_VSCROLL).toDouble())
                true
            }
            MotionEvent.ACTION_BUTTON_PRESS -> sendMouseButtonUnconverted(event.actionButton, true)
            MotionEvent.ACTION_BUTTON_RELEASE -> sendMouseButtonUnconverted(event.actionButton, false)
            else -> false
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        @Suppress("SENSELESS_COMPARISON")
        if (mCurrentTouchProcessor != null) {
            mCurrentTouchProcessor.dispatchTouchEvent(event, this)
        }
        return super.dispatchTouchEvent(event)
    }

    /** The event for keyboard/ gamepad button inputs */
    fun processKeyEvent(event: KeyEvent): Boolean {
        //Log.i("KeyEvent", event.toString());

        //Filtering useless events by order of probability
        val eventKeycode = event.keyCode
        if (eventKeycode == KeyEvent.KEYCODE_UNKNOWN) return true
        if (eventKeycode == KeyEvent.KEYCODE_VOLUME_DOWN) return false
        if (eventKeycode == KeyEvent.KEYCODE_VOLUME_UP) return false
        if (event.repeatCount != 0) return true
        val action = event.action
        if (action == KeyEvent.ACTION_MULTIPLE) return true
        // Ignore the cancelled up events. They occur when the user switches layouts.
        // In accordance with https://developer.android.com/reference/android/view/KeyEvent#FLAG_CANCELED
        if (action == KeyEvent.ACTION_UP &&
            (event.flags and KeyEvent.FLAG_CANCELED) != 0) return true

        //Sometimes, key events comes from SOME keys of the software keyboard
        //Even weirder, is is unknown why a key or another is selected to trigger a keyEvent
        if ((event.flags and KeyEvent.FLAG_SOFT_KEYBOARD) == KeyEvent.FLAG_SOFT_KEYBOARD) {
            if (eventKeycode == KeyEvent.KEYCODE_ENTER) return true //We already listen to it.
            MainActivity.touchCharInput.dispatchKeyEvent(event)
            return true
        }

        //Sometimes, key events may come from the mouse
        if (event.device != null &&
            ((event.source and InputDevice.SOURCE_MOUSE_RELATIVE) == InputDevice.SOURCE_MOUSE_RELATIVE ||
                    (event.source and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE)) {

            if (eventKeycode == KeyEvent.KEYCODE_BACK) {
                CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt(), event.action == KeyEvent.ACTION_DOWN)
                return true
            }
        }

        if (Gamepad.isGamepadEvent(event)) {
            if (mGamepad == null) createGamepad(this, event.device)

            mInputManager.handleKeyEventInput(context, event, mGamepad)
            return true
        }

        val index = EfficientAndroidLWJGLKeycode.getIndexByKey(eventKeycode)
        if (EfficientAndroidLWJGLKeycode.containsIndex(index)) {
            EfficientAndroidLWJGLKeycode.execKey(event, index)
            return true
        }

        // Some events will be generated an infinite number of times when no consumed
        return (event.flags and KeyEvent.FLAG_FALLBACK) == KeyEvent.FLAG_FALLBACK
    }

    /** Called when the size need to be set at any point during the surface lifecycle **/
    fun refreshSize() {
        val newWidth = Tools.getDisplayFriendlyRes(Tools.currentDisplayMetrics.widthPixels, AllStaticSettings.scaleFactor)
        val newHeight = Tools.getDisplayFriendlyRes(Tools.currentDisplayMetrics.heightPixels, AllStaticSettings.scaleFactor)
        if (newHeight < 1 || newWidth < 1) {
            Logging.e("MGLSurface", String.format(Locale.getDefault(), "Impossible resolution : %dx%d", newWidth, newHeight))
            return
        }
        CallbackBridge.windowWidth = newWidth
        CallbackBridge.windowHeight = newHeight
        val surface = mSurface
        if (surface == null) {
            Logging.w("MGLSurface", "Attempt to refresh size on null surface")
            return
        }
        if (AllSettings.alternateSurface.getValue()) {
            val view = surface as SurfaceView
            val holder = view.holder
            if (holder != null) {
                holder.setFixedSize(CallbackBridge.windowWidth, CallbackBridge.windowHeight)
            }
        } else {
            val view = surface as TextureView
            val surfaceTexture = view.surfaceTexture
            if (surfaceTexture != null) {
                surfaceTexture.setDefaultBufferSize(CallbackBridge.windowWidth, CallbackBridge.windowHeight)
            }
        }

        CallbackBridge.sendUpdateWindowSize(CallbackBridge.windowWidth, CallbackBridge.windowHeight)
        EventBus.getDefault().post(RefreshHotbarEvent())
    }

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private fun realStart(surface: Surface) {
        // Initial size set
        refreshSize()

        //Load Minecraft options:
        MCOptions.set("fullscreen", "false")
        MCOptions.set("overrideWidth", CallbackBridge.windowWidth.toString())
        MCOptions.set("overrideHeight", CallbackBridge.windowHeight.toString())
        MCOptions.save()
        MCOptions.mcScale

        JREUtils.setupBridgeWindow(surface)

        // The old code started a thread named "JVM Main thread" here; it parked in wait() until
        // the surface-ready listener was attached, then ran the game (and stayed blocked in it)
        // on that very thread. A coroutine parked on a dedicated single-thread dispatcher named
        // identically preserves both the thread name and the blocking-friendly behaviour, and
        // the dispatcher is closed when the game returns - just like the thread then ended.
        val gameThread = newSingleThreadContext("JVM Main thread")
        CoroutineScope(gameThread + SupervisorJob() + CoroutineName("JVM Main thread")).launch {
            try {
                // Wait until the listener is attached
                mSurfaceReadySignal.await().isReady()
            } catch (e: Throwable) {
                Tools.showError(context, e, true)
            } finally {
                gameThread.close()
            }
        }
    }

    override fun onGrabState(isGrabbing: Boolean) {
        post { updateGrabState(isGrabbing) }
    }

    private fun pickEventProcessor(isGrabbing: Boolean): TouchEventProcessor {
        return if (isGrabbing) mIngameProcessor else mInGUIProcessor
    }

    private fun updateGrabState(isGrabbing: Boolean) {
        if (mLastGrabState != isGrabbing) {
            mCurrentTouchProcessor.cancelPendingActions()
            mCurrentTouchProcessor = pickEventProcessor(isGrabbing)
            mLastGrabState = isGrabbing
        }
    }

    /** A small interface called when the listener is ready for the first time */
    fun interface SurfaceReadyListener {
        fun isReady()
    }

    fun setSurfaceReadyListener(listener: SurfaceReadyListener) {
        mSurfaceReadySignal.complete(listener)
    }

    fun interface OnRenderingStartedListener {
        fun isStarted()
    }

    fun setOnRenderingStartedListener(listener: OnRenderingStartedListener?) {
        mOnRenderingStartedListener = listener
    }

    companion object {
        /** Convert the mouse button, then send it
         * @return Whether the event was processed
         */
        @JvmStatic
        fun sendMouseButtonUnconverted(button: Int, status: Boolean): Boolean {
            var glfwButton = -256
            when (button) {
                MotionEvent.BUTTON_PRIMARY -> glfwButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt()
                MotionEvent.BUTTON_TERTIARY -> glfwButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE.toInt()
                MotionEvent.BUTTON_SECONDARY -> glfwButton = LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt()
            }
            if (glfwButton == -256) return false
            CallbackBridge.sendMouseButton(glfwButton, status)
            return true
        }
    }
}
