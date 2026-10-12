package me.shadow.eclipselauncher.ui.view

import android.animation.AnimatorInflater
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.color.MaterialColors
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.core.Tools

open class AnimButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.buttonStyle
) : AppCompatButton(context, attrs, defStyleAttr) {
    private val variant: Int

    init {
        isAllCaps = false
        val styleable = context.obtainStyledAttributes(attrs, R.styleable.AnimButton)
        variant = styleable.getInt(R.styleable.AnimButton_buttonVariant, VARIANT_NEUTRAL)
        styleable.recycle()
        setRipple()
        stateListAnimator = AnimatorInflater.loadStateListAnimator(context, R.xml.anim_scale)
        translationZ = Tools.dpToPx(4f)
        when (variant) {
            VARIANT_FILLED -> setTextColor(MaterialColors.getColor(context, R.attr.colorOnPrimary, Color.WHITE))
            VARIANT_TEXT -> setTextColor(MaterialColors.getColor(context, R.attr.colorPrimary, Color.BLACK))
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        post {
            pivotX = width / 2f
            pivotY = height / 2f
        }
    }

    private fun setRipple() {
        when (variant) {
            VARIANT_FILLED -> background = RippleDrawable(
                ColorStateList.valueOf(0x40FFFFFF),
                ResourcesCompat.getDrawable(resources, R.drawable.button_background_filled, context.theme),
                null
            )
            VARIANT_TEXT -> background = RippleDrawable(
                ColorStateList.valueOf(
                    (MaterialColors.getColor(context, R.attr.colorPrimary, 0) and 0x00FFFFFF) or (0x26 shl 24)
                ),
                null,
                ResourcesCompat.getDrawable(resources, R.drawable.ripple_mask_2dp, context.theme)
            )
            else -> background = RippleDrawable(
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.background_ripple_effect)),
                ResourcesCompat.getDrawable(resources, R.drawable.button_background, context.theme),
                null
            )
        }
    }

    private companion object {
        const val VARIANT_NEUTRAL = 0
        const val VARIANT_FILLED = 1
        const val VARIANT_TEXT = 2
    }
}