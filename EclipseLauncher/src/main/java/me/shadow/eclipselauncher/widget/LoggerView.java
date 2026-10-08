package me.shadow.eclipselauncher.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import me.shadow.eclipselauncher.anim.animations.Animations;
import me.shadow.eclipselauncher.databinding.ViewLoggerBinding;
import me.shadow.eclipselauncher.setting.AllSettings;
import me.shadow.eclipselauncher.utils.anim.ViewAnimUtils;

import me.shadow.eclipselauncher.pojav.Logger;

/**
 * A class able to display logs to the user.
 * It has support for the Logger class
 */
public class LoggerView extends ConstraintLayout {
    private Logger.eventLogListener mLogListener;
    private ViewLoggerBinding binding;
    private boolean isShowing = false;

    public LoggerView(@NonNull Context context) {
        this(context, null);
    }

    public LoggerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        // Triggers the log view shown state by default when viewing it
        binding.toggleLog.setChecked(visibility == VISIBLE);
    }

    public void toggleViewWithAnim() {
        setVisibilityWithAnim(!isShowing);
    }

    public void setVisibilityWithAnim(boolean visibility) {
        if (isShowing == visibility) return;
        isShowing = visibility;

        ViewAnimUtils.setViewAnim(this,
                visibility ? Animations.BounceInUp : Animations.SlideOutDown,
                (long) (AllSettings.getAnimationSpeed().getValue() * 0.7),
                () -> setVisibility(VISIBLE),
                () -> setVisibility(visibility ? VISIBLE : GONE));
    }

    /**
     * Force the log to be shown; clicking the close button triggers a callback
     */
    public void forceShow(OnCloseClickListener listener) {
        setVisibilityWithAnim(true);
        binding.cancel.setOnClickListener(v -> listener.onClick());
    }

    /**
     * Inflate the layout, and add component behaviors
     */
    private void init() {
        binding = ViewLoggerBinding.inflate(LayoutInflater.from(getContext()), this, true);

        binding.logView.setTypeface(Typeface.MONOSPACE);
        //TODO clamp the max text so it doesn't go oob
        binding.logView.setMaxLines(Integer.MAX_VALUE);
        binding.logView.setEllipsize(null);
        binding.logView.setVisibility(GONE);

        // Toggle log visibility
        binding.toggleLog.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    binding.logView.setVisibility(isChecked ? VISIBLE : GONE);
                    if (isChecked) {
                        Logger.setLogListener(mLogListener);
                    } else {
                        binding.logView.setText("");
                        Logger.setLogListener(null); // Makes the JNI code be able to skip expensive logger callbacks
                        // NOTE: was tested by rapidly smashing the log on/off button, no sync issues found :)
                    }
                });
        binding.toggleLog.setChecked(false);

        // Remove the loggerView from the user View
        binding.cancel.setOnClickListener(view -> setVisibilityWithAnim(false));

        // Set the scroll view
        binding.scroll.setKeepFocusing(true);

        //Set up the autoscroll switch
        binding.toggleAutoscroll.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    if (isChecked) binding.scroll.fullScroll(View.FOCUS_DOWN);
                    binding.scroll.setKeepFocusing(isChecked);
                }
        );
        binding.toggleAutoscroll.setChecked(true);

        // Listen to logs
        mLogListener = text -> {
            if (binding.logView.getVisibility() != VISIBLE) return;
            post(() -> {
                binding.logView.append(text + '\n');
                if (binding.scroll.isKeepFocusing())
                    binding.scroll.fullScroll(View.FOCUS_DOWN);
            });
        };
    }

    public ViewLoggerBinding getBinding() {
        return binding;
    }

    public interface OnCloseClickListener {
        void onClick();
    }
}
