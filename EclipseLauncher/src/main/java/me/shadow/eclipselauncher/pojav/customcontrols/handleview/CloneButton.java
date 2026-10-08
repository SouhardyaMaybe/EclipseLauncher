package me.shadow.eclipselauncher.pojav.customcontrols.handleview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import me.shadow.eclipselauncher.R;
import me.shadow.eclipselauncher.ui.view.AnimButton;

import me.shadow.eclipselauncher.pojav.customcontrols.buttons.ControlInterface;

@SuppressLint("AppCompatCustomView")
public class CloneButton extends AnimButton implements ActionButtonInterface {
    public CloneButton(Context context) {super(context); init();}
    public CloneButton(Context context, @Nullable AttributeSet attrs) {super(context, attrs); init();}

    public void init() {
        setOnClickListener(this);
        setText(R.string.generic_clone);
    }

    private ControlInterface mCurrentlySelectedButton = null;

    @Override
    public boolean shouldBeVisible() {
        return mCurrentlySelectedButton != null;
    }

    @Override
    public void setFollowedView(ControlInterface view) {
        mCurrentlySelectedButton = view;
    }

    @Override
    public void onClick() {
        if(mCurrentlySelectedButton == null) return;

        mCurrentlySelectedButton.cloneButton();
        mCurrentlySelectedButton.getControlLayoutParent().removeEditWindow();
    }
}
