package me.shadow.eclipselauncher.core.customcontrols.mouse

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

import me.shadow.eclipselauncher.anim.AnimPlayer
import me.shadow.eclipselauncher.anim.animations.Animations
import me.shadow.eclipselauncher.event.single.MCOptionChangeEvent
import me.shadow.eclipselauncher.event.single.RefreshHotbarEvent
import me.shadow.eclipselauncher.event.value.HotbarChangeEvent
import me.shadow.eclipselauncher.feature.MCOptions
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.setting.AllStaticSettings
import me.shadow.eclipselauncher.ui.subassembly.hotbar.HotbarType
import me.shadow.eclipselauncher.ui.subassembly.hotbar.HotbarUtils

import me.shadow.eclipselauncher.core.GrabListener
import me.shadow.eclipselauncher.core.LwjglGlfwKeycode
import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.utils.MathUtils

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.lwjgl.glfw.CallbackBridge

class HotbarView @JvmOverloads @Suppress("unused") constructor( // You suggested me this constructor, Android
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes), View.OnLayoutChangeListener, Runnable {
    private val mDoubleTapDetector = TapDetector(2, TapDetector.DETECTION_METHOD_DOWN)
    private var mParentView: View? = null
    private val mDropGesture = DropGesture(Handler(Looper.getMainLooper()))
    private val mGrabListener = object : GrabListener {
        override fun onGrabState(isGrabbing: Boolean) {
            mLastIndex = -1
            mDropGesture.cancel()
        }
    }

    private var mWidth = 0
    private var mLastIndex = -1
    private var mGuiScale = 0

    //When adjusting the detection box size, use this animator to play a fade animation, giving the user feedback on the current detection box bounds
    private val adjustAnimPlayer = AnimPlayer()

    init {
        init()
    }

    private fun init() {
        alpha = 0f
        setBackgroundColor(Color.parseColor("#80E64242"))
        adjustAnimPlayer.duration(800L)
        adjustAnimPlayer.apply(AnimPlayer.Entry(this, Animations.FadeOut))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        EventBus.getDefault().register(this)
        val parentView = parent
        if (parentView == null) return
        if (parentView is View) {
            mParentView = parentView
            parentView.addOnLayoutChangeListener(this)
        }
        adaptiveReset()
        CallbackBridge.addGrabListener(mGrabListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        CallbackBridge.removeGrabListener(mGrabListener)
        EventBus.getDefault().unregister(this)
    }

    /**
     * Refresh the detection when the Hotbar refresh event is observed
     * @param event the refresh event
     */
    @Subscribe
    fun event(event: RefreshHotbarEvent) {
        post(this)
    }

    /**
     * Refresh the detection when options.txt changes, because the GUI scale needs to be checked
     * @param event the refresh event
     */
    @Subscribe
    fun event(event: MCOptionChangeEvent) {
        mGuiScale = MCOptions.mcScale
        post(this)
    }

    /**
     * Listen for events where the detection box size is adjusted manually
     * @param event the change event
     */
    @Subscribe
    fun event(event: HotbarChangeEvent) {
        manualReset(event.width, event.height, true)
    }

    private fun getMarginLayoutParams(): ViewGroup.MarginLayoutParams {
        val layoutParams = getLayoutParams()
        if (layoutParams !is ViewGroup.MarginLayoutParams)
            throw RuntimeException("Incorrect LayoutParams type, expected ViewGroup.MarginLayoutParams")
        return layoutParams
    }

    private fun adaptiveReset() {
        val marginLayoutParams = getMarginLayoutParams()
        mWidth = mcScale(180)
        marginLayoutParams.width = mWidth
        val height = mcScale(20)
        marginLayoutParams.height = height
        marginLayoutParams.leftMargin = (CallbackBridge.physicalWidth / 2) - (mWidth / 2)
        marginLayoutParams.topMargin = CallbackBridge.physicalHeight - height
        layoutParams = marginLayoutParams
    }

    private fun manualReset(width: Int, height: Int, playAnim: Boolean) {
        val marginLayoutParams = getMarginLayoutParams()
        mWidth = width
        marginLayoutParams.width = mWidth
        marginLayoutParams.height = height
        marginLayoutParams.leftMargin = Tools.currentDisplayMetrics.widthPixels / 2 - width / 2
        marginLayoutParams.topMargin = Tools.currentDisplayMetrics.heightPixels - height
        layoutParams = marginLayoutParams
        if (playAnim) adjustAnimPlayer.start()
    }

    @Suppress("ClickableViewAccessibility") // performClick does not report coordinates.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!CallbackBridge.isGrabbing()) return false
        val hasDoubleTapped = mDoubleTapDetector.onTouchEvent(event)

        // Check if we need to cancel the drop event
        val actionMasked = event.actionMasked
        if (isLastEventInGesture(actionMasked)) mDropGesture.cancel()
        else mDropGesture.submit()
        // Determine the hotbar slot
        val x = event.x
        // Ignore positions equal to mWidth because they would translate into an out-of-bounds hotbar index
        if (x < 0 || x >= mWidth) {
            // If out of bounds, cancel the hotbar gesture to avoid dropping items on last hotbar slots
            mDropGesture.cancel()
            return true
        }
        val hotbarIndex = MathUtils.map(x, 0f, mWidth.toFloat(), 0f, HOTBAR_KEYS.size.toFloat()).toInt()
        // Check if the slot changed and we need to make a key press
        if (hotbarIndex == mLastIndex) {
            // Only check for doubletapping if the slot has not changed
            if (hasDoubleTapped && !AllStaticSettings.disableDoubleTap) CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_F.toInt())
            return true
        }
        mLastIndex = hotbarIndex
        val hotbarKey = HOTBAR_KEYS[hotbarIndex]
        CallbackBridge.sendKeyPress(hotbarKey)
        // Cancel the event since we changed hotbar slots.
        mDropGesture.cancel()
        // Only resubmit the gesture only if it isn't the last event we will receive.
        if (!isLastEventInGesture(actionMasked)) mDropGesture.submit()
        return true
    }

    private fun isLastEventInGesture(actionMasked: Int): Boolean {
        return actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL
    }

    private fun mcScale(input: Int): Int {
        return ((mGuiScale * input) / AllStaticSettings.scaleFactor).toInt()
    }

    override fun run() {
        if (parent == null) return

        val hotbarType = HotbarUtils.getCurrentType()
        if (hotbarType == HotbarType.AUTO) {
            adaptiveReset()
        } else {
            manualReset(AllSettings.hotbarWidth.value.getValue(), AllSettings.hotbarHeight.value.getValue(), false)
        }
    }

    override fun onLayoutChange(
        v: View, left: Int, top: Int, right: Int, bottom: Int,
        oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
    ) {
        // We need to check whether dimensions match or not because here we are looking specifically for changes of dimensions
        // and Android keeps calling this without dimensions actually changing for some reason.
        if (v == mParentView && (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom)) {
            // Need to post this, because it is not correct to resize the view
            // during a layout pass.
            post(this::adaptiveReset)
        }
    }

    companion object {
        private val HOTBAR_KEYS = intArrayOf(
            LwjglGlfwKeycode.GLFW_KEY_1.toInt(), LwjglGlfwKeycode.GLFW_KEY_2.toInt(), LwjglGlfwKeycode.GLFW_KEY_3.toInt(),
            LwjglGlfwKeycode.GLFW_KEY_4.toInt(), LwjglGlfwKeycode.GLFW_KEY_5.toInt(), LwjglGlfwKeycode.GLFW_KEY_6.toInt(),
            LwjglGlfwKeycode.GLFW_KEY_7.toInt(), LwjglGlfwKeycode.GLFW_KEY_8.toInt(), LwjglGlfwKeycode.GLFW_KEY_9.toInt()
        )
    }
}
