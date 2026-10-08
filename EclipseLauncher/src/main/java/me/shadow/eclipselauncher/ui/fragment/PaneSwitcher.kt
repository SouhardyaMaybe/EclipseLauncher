package me.shadow.eclipselauncher.ui.fragment

import android.content.res.Configuration
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import me.shadow.eclipselauncher.pojav.Tools
import me.shadow.eclipselauncher.ui.view.AnimButton

/**
 * Gives a two-pane layout a single-pane portrait mode.
 *
 * - [Mode.OVERLAY]: the left pane fills the screen while the right pane becomes a
 *   hidden full-screen overlay, opened with the floating opener button.
 * - [Mode.SWITCH]: exactly one of the two panes is visible at a time; the opener
 *   button swaps between them.
 * - [Mode.RAIL]: the left pane (a vertical tab rail) is hidden and the right pane
 *   expands into its place; the opener button is provided for a custom action.
 * - [Mode.PRIMARY_RIGHT]: the right pane becomes the only visible pane (no opener).
 * - [Mode.STACK_BOTTOM]: the panes stack vertically; the right pane becomes a
 *   wrap-content bar along the bottom edge (no opener).
 *
 * Rotating back to landscape restores the original two-pane layout untouched.
 */
class PaneSwitcher(
    val root: View,
    @IdRes private val leftPaneId: Int,
    @IdRes private val rightPaneId: Int,
    @IdRes openerLabelId: Int = 0,
    private val mode: Mode = Mode.OVERLAY
) {
    enum class Mode { OVERLAY, SWITCH, RAIL, PRIMARY_RIGHT, STACK_BOTTOM }

    private val leftPane: View? = root.findViewById(leftPaneId)
    private val rightPane: View? = root.findViewById(rightPaneId)

    /** Floating portrait-only button that swaps the panes; null when not requested */
    val openerButton: AnimButton? = if (openerLabelId != 0) {
        AnimButton(root.context).apply {
            setText(openerLabelId)
            textSize = 12f
            val margin = Tools.dpToPx(12f).toInt()
            val padH = Tools.dpToPx(16f).toInt()
            val padV = Tools.dpToPx(8f).toInt()
            setPadding(padH, padV, padH, padV)
            visibility = View.GONE
            layoutParams = when (root) {
                is ConstraintLayout -> ConstraintLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    marginEnd = margin
                    bottomMargin = margin
                }
                is FrameLayout -> FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                    setMargins(margin, margin, margin, margin)
                }
                else -> ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            if (root is ViewGroup) root.addView(this)
        }
    } else null

    // Landscape constraint/visibility snapshots taken when entering portrait
    private var savedLeftStartToStart = 0
    private var savedLeftStartToEnd = 0
    private var savedLeftEndToStart = 0
    private var savedLeftEndToEnd = 0
    private var savedLeftBottomToBottom = 0
    private var savedLeftBottomToTop = 0
    private var savedRightStartToStart = 0
    private var savedRightStartToEnd = 0
    private var savedRightEndToStart = 0
    private var savedRightEndToEnd = 0
    private var savedRightTopToTop = 0
    private var savedRightTopToBottom = 0
    private var savedRightHeight = 0
    private var savedLeftVisibility = View.VISIBLE
    private var savedRightVisibility = View.VISIBLE
    private var portraitActive = false

    /** Whether a pane is currently covering the primary content (portrait only) */
    val isPaneOpen: Boolean
        get() = portraitActive && when (mode) {
            Mode.OVERLAY, Mode.SWITCH -> rightPane?.visibility == View.VISIBLE
            else -> false
        }

    /**
     * Applies or removes single-pane mode for the given configuration.
     * Safe to call multiple times with the same configuration.
     */
    fun applyOrientation(config: Configuration) {
        val left = leftPane ?: return
        val right = rightPane ?: return
        val portrait = config.orientation == Configuration.ORIENTATION_PORTRAIT
        if (portrait == portraitActive) return
        portraitActive = portrait
        if (portrait) enterPortrait(left, right) else exitPortrait(left, right)
        root.requestLayout()
    }

    /** Reveals the secondary pane. Does nothing in landscape. */
    fun openPane() {
        if (!portraitActive) return
        when (mode) {
            Mode.OVERLAY -> rightPane?.visibility = View.VISIBLE
            Mode.SWITCH -> {
                rightPane?.visibility = View.VISIBLE
                leftPane?.visibility = View.GONE
            }
            else -> Unit
        }
    }

    /** Hides the secondary pane again; returns true when a visible pane was hidden. */
    fun closePane(): Boolean {
        if (!portraitActive) return false
        return when (mode) {
            Mode.OVERLAY -> {
                if (rightPane?.visibility != View.VISIBLE) false
                else {
                    rightPane.visibility = View.GONE
                    true
                }
            }
            Mode.SWITCH -> {
                if (rightPane?.visibility != View.VISIBLE) false
                else {
                    rightPane.visibility = View.GONE
                    leftPane?.visibility = View.VISIBLE
                    true
                }
            }
            else -> false
        }
    }

    /** Default opener action: show/hide the secondary pane. */
    fun togglePane() {
        if (isPaneOpen) closePane() else openPane()
    }

    private fun enterPortrait(left: View, right: View) {
        val leftParams = left.layoutParams
        val rightParams = right.layoutParams
        val constraintLayout = leftParams is ConstraintLayout.LayoutParams &&
                rightParams is ConstraintLayout.LayoutParams

        savedLeftVisibility = left.visibility
        savedRightVisibility = right.visibility

        if (constraintLayout) {
            val lp = leftParams as ConstraintLayout.LayoutParams
            val rp = rightParams as ConstraintLayout.LayoutParams
            savedLeftStartToStart = lp.startToStart
            savedLeftStartToEnd = lp.startToEnd
            savedLeftEndToStart = lp.endToStart
            savedLeftEndToEnd = lp.endToEnd
            savedLeftBottomToBottom = lp.bottomToBottom
            savedLeftBottomToTop = lp.bottomToTop
            savedRightStartToStart = rp.startToStart
            savedRightStartToEnd = rp.startToEnd
            savedRightEndToStart = rp.endToStart
            savedRightEndToEnd = rp.endToEnd
            savedRightTopToTop = rp.topToTop
            savedRightTopToBottom = rp.topToBottom
            savedRightHeight = rp.height
        }

        when (mode) {
            Mode.OVERLAY -> {
                if (constraintLayout) {
                    fillParent(leftParams as ConstraintLayout.LayoutParams)
                    fillParent(rightParams as ConstraintLayout.LayoutParams)
                }
                right.visibility = View.GONE
                openerButton?.visibility = View.VISIBLE
            }
            Mode.SWITCH -> {
                if (constraintLayout) {
                    fillParent(leftParams as ConstraintLayout.LayoutParams)
                    fillParent(rightParams as ConstraintLayout.LayoutParams)
                }
                left.visibility = View.VISIBLE
                right.visibility = View.GONE
                openerButton?.visibility = View.VISIBLE
            }
            Mode.RAIL -> {
                if (constraintLayout) {
                    fillParent(rightParams as ConstraintLayout.LayoutParams)
                }
                left.visibility = View.GONE
                openerButton?.visibility = View.VISIBLE
            }
            Mode.PRIMARY_RIGHT -> {
                if (constraintLayout) {
                    fillParent(leftParams as ConstraintLayout.LayoutParams)
                    fillParent(rightParams as ConstraintLayout.LayoutParams)
                }
                left.visibility = View.GONE
            }
            Mode.STACK_BOTTOM -> {
                if (constraintLayout) {
                    val lp = leftParams as ConstraintLayout.LayoutParams
                    val rp = rightParams as ConstraintLayout.LayoutParams
                    fillParent(lp)
                    fillParent(rp)
                    // The left pane stretches across the top, the right pane becomes
                    // a wrap-content bar hugging the bottom edge
                    lp.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                    lp.bottomToTop = right.id
                    rp.topToTop = ConstraintLayout.LayoutParams.UNSET
                    rp.topToBottom = ConstraintLayout.LayoutParams.UNSET
                    rp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }
        }
    }

    private fun exitPortrait(left: View, right: View) {
        val leftParams = left.layoutParams
        val rightParams = right.layoutParams
        if (leftParams is ConstraintLayout.LayoutParams && rightParams is ConstraintLayout.LayoutParams) {
            leftParams.startToStart = savedLeftStartToStart
            leftParams.startToEnd = savedLeftStartToEnd
            leftParams.endToStart = savedLeftEndToStart
            leftParams.endToEnd = savedLeftEndToEnd
            leftParams.bottomToBottom = savedLeftBottomToBottom
            leftParams.bottomToTop = savedLeftBottomToTop
            rightParams.startToStart = savedRightStartToStart
            rightParams.startToEnd = savedRightStartToEnd
            rightParams.endToStart = savedRightEndToStart
            rightParams.endToEnd = savedRightEndToEnd
            rightParams.topToTop = savedRightTopToTop
            rightParams.topToBottom = savedRightTopToBottom
            rightParams.height = savedRightHeight
        }
        left.visibility = savedLeftVisibility
        right.visibility = savedRightVisibility
        openerButton?.visibility = View.GONE
    }

    /** Stretches a pane across the full width of its parent. */
    private fun fillParent(params: ConstraintLayout.LayoutParams) {
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        params.startToEnd = ConstraintLayout.LayoutParams.UNSET
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        params.endToStart = ConstraintLayout.LayoutParams.UNSET
    }
}
