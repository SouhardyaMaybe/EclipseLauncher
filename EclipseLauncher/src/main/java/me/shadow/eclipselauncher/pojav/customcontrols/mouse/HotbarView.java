package me.shadow.eclipselauncher.pojav.customcontrols.mouse;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.Nullable;

import me.shadow.eclipselauncher.anim.AnimPlayer;
import me.shadow.eclipselauncher.anim.animations.Animations;
import me.shadow.eclipselauncher.event.single.MCOptionChangeEvent;
import me.shadow.eclipselauncher.event.single.RefreshHotbarEvent;
import me.shadow.eclipselauncher.event.value.HotbarChangeEvent;
import me.shadow.eclipselauncher.feature.MCOptions;
import me.shadow.eclipselauncher.setting.AllSettings;
import me.shadow.eclipselauncher.setting.AllStaticSettings;
import me.shadow.eclipselauncher.ui.subassembly.hotbar.HotbarType;
import me.shadow.eclipselauncher.ui.subassembly.hotbar.HotbarUtils;

import me.shadow.eclipselauncher.pojav.GrabListener;
import me.shadow.eclipselauncher.pojav.LwjglGlfwKeycode;
import me.shadow.eclipselauncher.pojav.Tools;
import me.shadow.eclipselauncher.pojav.utils.MathUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.lwjgl.glfw.CallbackBridge;

public class HotbarView extends View implements View.OnLayoutChangeListener, Runnable {
    private final TapDetector mDoubleTapDetector = new TapDetector(2, TapDetector.DETECTION_METHOD_DOWN);
    private View mParentView;
    private static final int[] HOTBAR_KEYS = {
            LwjglGlfwKeycode.GLFW_KEY_1, LwjglGlfwKeycode.GLFW_KEY_2,   LwjglGlfwKeycode.GLFW_KEY_3,
            LwjglGlfwKeycode.GLFW_KEY_4, LwjglGlfwKeycode.GLFW_KEY_5,   LwjglGlfwKeycode.GLFW_KEY_6,
            LwjglGlfwKeycode.GLFW_KEY_7, LwjglGlfwKeycode.GLFW_KEY_8, LwjglGlfwKeycode.GLFW_KEY_9};
    private final DropGesture mDropGesture = new DropGesture(new Handler(Looper.getMainLooper()));
    private final GrabListener mGrabListener = new GrabListener() {
        @Override
        public void onGrabState(boolean isGrabbing) {
            mLastIndex = -1;
            mDropGesture.cancel();
        }
    };

    private int mWidth;
    private int mLastIndex = -1;
    private int mGuiScale;

    //When adjusting the detection box size, use this animator to play a fade animation, giving the user feedback on the current detection box bounds
    private final AnimPlayer adjustAnimPlayer = new AnimPlayer();

    public HotbarView(Context context) {
        super(context);
        init();
    }

    public HotbarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HotbarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressWarnings("unused") // You suggested me this constructor, Android
    public HotbarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setAlpha(0);
        setBackgroundColor(Color.parseColor("#80E64242"));
        adjustAnimPlayer.duration(800);
        adjustAnimPlayer.apply(new AnimPlayer.Entry(this, Animations.FadeOut));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
        ViewParent parent = getParent();
        if(parent == null) return;
        if(parent instanceof View) {
            mParentView = (View) parent;
            mParentView.addOnLayoutChangeListener(this);
        }
        adaptiveReset();
        CallbackBridge.addGrabListener(mGrabListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        CallbackBridge.removeGrabListener(mGrabListener);
        EventBus.getDefault().unregister(this);
    }

    /**
     * Refresh the detection when the Hotbar refresh event is observed
     * @param event the refresh event
     */
    @Subscribe
    public void event(RefreshHotbarEvent event) {
        post(this);
    }

    /**
     * Refresh the detection when options.txt changes, because the GUI scale needs to be checked
     * @param event the refresh event
     */
    @Subscribe
    public void event(MCOptionChangeEvent event) {
        mGuiScale = MCOptions.INSTANCE.getMcScale();
        post(this);
    }

    /**
     * Listen for events where the detection box size is adjusted manually
     * @param event the change event
     */
    @Subscribe
    public void event(HotbarChangeEvent event) {
        manualReset(event.getWidth(), event.getHeight(), true);
    }

    private ViewGroup.MarginLayoutParams getMarginLayoutParams() {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams))
            throw new RuntimeException("Incorrect LayoutParams type, expected ViewGroup.MarginLayoutParams");
        return (ViewGroup.MarginLayoutParams) layoutParams;
    }

    private void adaptiveReset() {
        ViewGroup.MarginLayoutParams marginLayoutParams = getMarginLayoutParams();
        int height;
        marginLayoutParams.width = mWidth = mcScale(180);
        marginLayoutParams.height = height = mcScale(20);
        marginLayoutParams.leftMargin = (CallbackBridge.physicalWidth / 2) - (mWidth / 2);
        marginLayoutParams.topMargin = CallbackBridge.physicalHeight - height;
        setLayoutParams(marginLayoutParams);
    }

    private void manualReset(int width, int height, boolean playAnim) {
        ViewGroup.MarginLayoutParams marginLayoutParams = getMarginLayoutParams();
        marginLayoutParams.width = mWidth = width;
        marginLayoutParams.height = height;
        marginLayoutParams.leftMargin = Tools.currentDisplayMetrics.widthPixels / 2 - width / 2;
        marginLayoutParams.topMargin = Tools.currentDisplayMetrics.heightPixels - height;
        setLayoutParams(marginLayoutParams);
        if (playAnim) adjustAnimPlayer.start();
    }

    @SuppressWarnings("ClickableViewAccessibility") // performClick does not report coordinates.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!CallbackBridge.isGrabbing()) return false;
        boolean hasDoubleTapped = mDoubleTapDetector.onTouchEvent(event);

        // Check if we need to cancel the drop event
        int actionMasked = event.getActionMasked();
        if(isLastEventInGesture(actionMasked)) mDropGesture.cancel();
        else mDropGesture.submit();
        // Determine the hotbar slot
        float x = event.getX();
        // Ignore positions equal to mWidth because they would translate into an out-of-bounds hotbar index
        if(x < 0 || x >= mWidth) {
            // If out of bounds, cancel the hotbar gesture to avoid dropping items on last hotbar slots
            mDropGesture.cancel();
            return true;
        }
        int hotbarIndex = (int)MathUtils.map(x, 0, mWidth, 0, HOTBAR_KEYS.length);
        // Check if the slot changed and we need to make a key press
        if(hotbarIndex == mLastIndex) {
            // Only check for doubletapping if the slot has not changed
            if (hasDoubleTapped && !AllStaticSettings.disableDoubleTap) CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_F);
            return true;
        }
        mLastIndex = hotbarIndex;
        int hotbarKey = HOTBAR_KEYS[hotbarIndex];
        CallbackBridge.sendKeyPress(hotbarKey);
        // Cancel the event since we changed hotbar slots.
        mDropGesture.cancel();
        // Only resubmit the gesture only if it isn't the last event we will receive.
        if(!isLastEventInGesture(actionMasked)) mDropGesture.submit();
        return true;
    }

    private boolean isLastEventInGesture(int actionMasked) {
        return actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL;
    }

    private int mcScale(int input) {
        return (int)((mGuiScale * input)/ AllStaticSettings.scaleFactor);
    }

    @Override
    public void run() {
        if (getParent() == null) return;

        HotbarType hotbarType = HotbarUtils.getCurrentType();
        if (hotbarType == HotbarType.AUTO) {
            adaptiveReset();
        } else {
            manualReset(AllSettings.getHotbarWidth().getValue().getValue(), AllSettings.getHotbarHeight().getValue().getValue(), false);
        }
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        // We need to check whether dimensions match or not because here we are looking specifically for changes of dimensions
        // and Android keeps calling this without dimensions actually changing for some reason.
        if(v.equals(mParentView) && (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom)) {
            // Need to post this, because it is not correct to resize the view
            // during a layout pass.
            post(this::adaptiveReset);
        }
    }
}
