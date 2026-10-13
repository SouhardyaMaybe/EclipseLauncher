package me.shadow.eclipselauncher.mcgui;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import me.shadow.eclipselauncher.R;

/**
 * Progress row used by {@link ProgressLayout}: a label beside a Material 3
 * determinate indicator. Keeps the setText/setProgress/setTextPadding surface
 * that ProgressLayout drives it with.
 */
public class TextProgressBar extends LinearLayout {

    private final TextView mLabel;
    private final LinearProgressIndicator mIndicator;

    public TextProgressBar(Context context) {
        super(context);
        init();
    }

    public TextProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TextProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        mLabel = new TextView(getContext());
        mLabel.setSingleLine(true);
        mLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(R.dimen._11ssp));
        mLabel.setPaintFlags(mLabel.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
        mLabel.setTextColor(MaterialColors.getColor(this, R.attr.colorOnSurface));
        mLabel.setPadding(0, 0, 0, 0);
        addView(mLabel, new LayoutParams(0, LayoutParams.MATCH_PARENT, 2f));

        mIndicator = new LinearProgressIndicator(getContext());
        mIndicator.setIndeterminate(false);
        mIndicator.setIndicatorColor(MaterialColors.getColor(this, R.attr.colorPrimary));
        mIndicator.setTrackColor(MaterialColors.getColor(this, R.attr.colorSurfaceContainerHighest));
        addView(mIndicator, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));

        setProgress(0);
    }

    /** 0..100, animated like every other Material indicator in the launcher. */
    public final void setProgress(int progress) {
        mIndicator.setProgressCompat(Math.max(0, Math.min(100, progress)), true);
    }

    public final void setText(@StringRes int resid) {
        setText(getContext().getResources().getText(resid).toString());
    }

    public final void setText(String text) {
        mLabel.setText(text);
    }

    public final void setTextPadding(int padding) {
        mLabel.setPadding(padding, 0, padding, 0);
    }
}
