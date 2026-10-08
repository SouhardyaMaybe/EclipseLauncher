package me.shadow.eclipselauncher.ui.fragment

import android.content.res.Configuration
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import me.shadow.eclipselauncher.pojav.Tools
import me.shadow.eclipselauncher.ui.view.AnimButton

/**
 * Gives a two-pane layout a portrait single-pane presentation.
 *
 * All landscape values (widths, visibility, and for [Mode.STACK_BOTTOM] the
 * horizontal constraints) are captured once in the constructor, which always runs
 * on a freshly inflated view. Applying a mode only writes known values, so the
 * switcher is idempotent: applying the same orientation twice, or re-entering
 * after a missed callback, can never corrupt the landscape snapshot.
 *
 * - [Mode.ICON_RAIL]: both panes stay side by side; the left pane narrows into an
 *   icon-only sidebar (labels hidden, icons centered).
 * - [Mode.HEADERS]: the left pane (a vertical section list) fills the screen as a
 *   headers screen; opening a section hides it and reveals the right pane.
 * - [Mode.OVERLAY] / [Mode.SWITCH]: portrait shows the left pane full width; the
 *   opener button swaps to the right pane (right fills automatically because the
 *   left pane goes away inside its chain).
 * - [Mode.PRIMARY_RIGHT]: the right pane becomes the only visible pane (no opener).
 * - [Mode.STACK_BOTTOM]: the panes stack vertically; the right pane becomes a
 *   wrap-content bar along the bottom edge (no opener).
 */
class PaneSwitcher(
    val root: View,
    @IdRes private val leftPaneId: Int,
    @IdRes private val rightPaneId: Int,
    @IdRes openerLabelId: Int = 0,
    private val mode: Mode = Mode.OVERLAY
) {
    enum class Mode { ICON_RAIL, HEADERS, OVERLAY, SWITCH, PRIMARY_RIGHT, STACK_BOTTOM }

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

    // ---- pristine landscape values, captured once on the fresh inflated view ----
    private var origLeftWidth = 0
    private var origRightWidth = 0
    private var origLeftVisibility = View.VISIBLE
    private var origRightVisibility = View.VISIBLE

    private var stackSaved: StackSnapshot? = null
    private val railRows: List<RailRow> =
        if (mode == Mode.ICON_RAIL) collectRailRows(leftPane) else emptyList()

    /**
     * Wrap-content children of the headers pane (the vertical tab strip). In the
     * landscape rail they hug their content; as a full-screen headers list they
     * must stretch to match_parent. Their original width is always wrap_content
     * by definition, so restoring is a constant write.
     */
    private val headerStretchChildren: List<View> = run {
        val left = leftPane
        if (mode == Mode.HEADERS && left is ViewGroup) {
            (0 until left.childCount).map { left.getChildAt(it) }
                .filter { it.layoutParams?.width == ViewGroup.LayoutParams.WRAP_CONTENT }
        } else emptyList()
    }

    private var portraitActive = false

    private val railWidthPx: Int = Tools.dpToPx(60f).toInt()
    private val railRowMarginPx: Int = Tools.dpToPx(4f).toInt()

    init {
        val left = leftPane
        val right = rightPane
        if (left != null && right != null) {
            origLeftWidth = left.layoutParams?.width ?: 0
            origRightWidth = right.layoutParams?.width ?: 0
            origLeftVisibility = left.visibility
            origRightVisibility = right.visibility
            if (mode == Mode.STACK_BOTTOM) {
                val lp = left.layoutParams
                val rp = right.layoutParams
                if (lp is ConstraintLayout.LayoutParams && rp is ConstraintLayout.LayoutParams) {
                    stackSaved = StackSnapshot(
                        lp.startToStart, lp.startToEnd, lp.endToStart, lp.endToEnd,
                        lp.bottomToBottom, lp.bottomToTop,
                        rp.startToStart, rp.startToEnd, rp.endToStart, rp.endToEnd,
                        rp.topToTop, rp.topToBottom, rp.height
                    )
                }
            }
        }
    }

    /** Whether a pane is currently covering the primary content (portrait only) */
    val isPaneOpen: Boolean
        get() = portraitActive && when (mode) {
            Mode.OVERLAY, Mode.SWITCH, Mode.HEADERS -> rightPane?.visibility == View.VISIBLE
            else -> false
        }

    /**
     * Applies or removes the portrait presentation for the given configuration.
     * Safe to call any number of times with any configuration.
     */
    fun applyOrientation(config: Configuration) {
        val left = leftPane ?: return
        val right = rightPane ?: return
        val portrait = config.orientation == Configuration.ORIENTATION_PORTRAIT
        if (portrait == portraitActive) return
        portraitActive = portrait
        if (portrait) applyPortrait(left, right) else applyLandscape(left, right)
        root.requestLayout()
    }

    /** Reveals the secondary pane. Does nothing in landscape. */
    fun openPane() {
        if (!portraitActive) return
        val left = leftPane ?: return
        val right = rightPane ?: return
        when (mode) {
            Mode.OVERLAY, Mode.SWITCH, Mode.HEADERS -> {
                keepFillWidth(right, origRightWidth)
                right.visibility = View.VISIBLE
                left.visibility = View.GONE
            }
            else -> Unit
        }
        root.requestLayout()
    }

    /** Hides the secondary pane again; returns true when a visible pane was hidden. */
    fun closePane(): Boolean {
        if (!portraitActive) return false
        val left = leftPane ?: return false
        val right = rightPane ?: return false
        return when (mode) {
            Mode.OVERLAY, Mode.SWITCH, Mode.HEADERS -> {
                if (right.visibility != View.VISIBLE) false
                else {
                    keepFillWidth(left, origLeftWidth)
                    right.visibility = View.GONE
                    left.visibility = View.VISIBLE
                    root.requestLayout()
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

    private fun applyPortrait(left: View, right: View) {
        when (mode) {
            Mode.ICON_RAIL -> {
                left.layoutParams?.let { it.width = railWidthPx }
                railRows.forEach { applyRailRow(it) }
                // the play pane keeps its 0dp chain width and fills the rest
            }
            Mode.HEADERS -> {
                keepFillWidth(left, origLeftWidth)
                left.visibility = View.VISIBLE
                right.visibility = View.GONE
                headerStretchChildren.forEach { it.layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT }
            }
            Mode.OVERLAY, Mode.SWITCH -> {
                keepFillWidth(left, origLeftWidth)
                left.visibility = View.VISIBLE
                right.visibility = View.GONE
                openerButton?.visibility = View.VISIBLE
            }
            Mode.PRIMARY_RIGHT -> {
                keepFillWidth(right, origRightWidth)
                right.visibility = View.VISIBLE
                left.visibility = View.GONE
            }
            Mode.STACK_BOTTOM -> applyStack(left, right)
        }
    }

    private fun applyLandscape(left: View, right: View) {
        left.layoutParams?.let { it.width = origLeftWidth }
        right.layoutParams?.let { it.width = origRightWidth }
        left.visibility = origLeftVisibility
        right.visibility = origRightVisibility
        headerStretchChildren.forEach { it.layoutParams?.width = ViewGroup.LayoutParams.WRAP_CONTENT }
        railRows.forEach { restoreRailRow(it) }
        if (mode == Mode.STACK_BOTTOM) restoreStack(left, right)
        openerButton?.visibility = View.GONE
    }

    /**
     * The showing pane must span the full width: chain panes (width 0dp) already
     * do so once their partner is gone; wrapped panes are widened to match_parent.
     * The value written depends only on the pristine width, so repeat calls are safe.
     */
    private fun keepFillWidth(view: View, origWidth: Int) {
        val lp = view.layoutParams ?: return
        lp.width = if (origWidth == 0) 0 else ViewGroup.LayoutParams.MATCH_PARENT
    }

    private fun applyStack(left: View, right: View) {
        val lp = left.layoutParams as? ConstraintLayout.LayoutParams ?: return
        val rp = right.layoutParams as? ConstraintLayout.LayoutParams ?: return
        restoreStack(left, right) // start from the snapshot so re-entry is idempotent
        lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        lp.startToEnd = ConstraintLayout.LayoutParams.UNSET
        lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        lp.endToStart = ConstraintLayout.LayoutParams.UNSET
        lp.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
        lp.bottomToTop = right.id
        rp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        rp.startToEnd = ConstraintLayout.LayoutParams.UNSET
        rp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        rp.endToStart = ConstraintLayout.LayoutParams.UNSET
        rp.topToTop = ConstraintLayout.LayoutParams.UNSET
        rp.topToBottom = ConstraintLayout.LayoutParams.UNSET
        rp.height = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    private fun restoreStack(left: View, right: View) {
        val snapshot = stackSaved ?: return
        val lp = left.layoutParams as? ConstraintLayout.LayoutParams ?: return
        val rp = right.layoutParams as? ConstraintLayout.LayoutParams ?: return
        lp.startToStart = snapshot.leftStartToStart
        lp.startToEnd = snapshot.leftStartToEnd
        lp.endToStart = snapshot.leftEndToStart
        lp.endToEnd = snapshot.leftEndToEnd
        lp.bottomToBottom = snapshot.leftBottomToBottom
        lp.bottomToTop = snapshot.leftBottomToTop
        rp.startToStart = snapshot.rightStartToStart
        rp.startToEnd = snapshot.rightStartToEnd
        rp.endToStart = snapshot.rightEndToStart
        rp.endToEnd = snapshot.rightEndToEnd
        rp.topToTop = snapshot.rightTopToTop
        rp.topToBottom = snapshot.rightTopToBottom
        rp.height = snapshot.rightHeight
    }

    // ---- icon-rail row handling (home navigation menu) ----

    private class StackSnapshot(
        val leftStartToStart: Int, val leftStartToEnd: Int,
        val leftEndToStart: Int, val leftEndToEnd: Int,
        val leftBottomToBottom: Int, val leftBottomToTop: Int,
        val rightStartToStart: Int, val rightStartToEnd: Int,
        val rightEndToStart: Int, val rightEndToEnd: Int,
        val rightTopToTop: Int, val rightTopToBottom: Int,
        val rightHeight: Int
    )

    private class RailRow(
        val row: ViewGroup,
        val layoutParams: ViewGroup.MarginLayoutParams,
        val origMarginStart: Int,
        val origMarginEnd: Int,
        val texts: List<TextView>,
        val origTextVisibility: List<Int>,
        val icons: List<ImageView>,
        val origAlignParentStart: List<Boolean>,
        val origCenterHorizontal: List<Boolean>,
        val origIconMarginStart: List<Int>
    )

    private fun collectRailRows(menu: View?): List<RailRow> {
        if (menu !is ViewGroup) return emptyList()
        val rows = ArrayList<RailRow>()
        fun walk(view: View) {
            if (view !is ViewGroup) return
            val texts = ArrayList<TextView>()
            val icons = ArrayList<ImageView>()
            for (i in 0 until view.childCount) {
                when (val child = view.getChildAt(i)) {
                    is TextView -> texts.add(child)
                    is ImageView -> icons.add(child)
                }
            }
            val lp = view.layoutParams
            if (texts.isNotEmpty() && icons.isNotEmpty() && lp is ViewGroup.MarginLayoutParams) {
                rows.add(
                    RailRow(
                        view, lp, lp.marginStart, lp.marginEnd,
                        texts, texts.map { it.visibility },
                        icons,
                        icons.map { icon ->
                            val rl = icon.layoutParams as? RelativeLayout.LayoutParams
                            rl != null && rl.getRules()[RelativeLayout.ALIGN_PARENT_START] != 0
                        },
                        icons.map { icon ->
                            val rl = icon.layoutParams as? RelativeLayout.LayoutParams
                            rl != null && rl.getRules()[RelativeLayout.CENTER_HORIZONTAL] != 0
                        },
                        icons.map { (it.layoutParams as? RelativeLayout.LayoutParams)?.marginStart ?: 0 }
                    )
                )
                return
            }
            for (i in 0 until view.childCount) walk(view.getChildAt(i))
        }
        walk(menu)
        return rows
    }

    private fun applyRailRow(entry: RailRow) {
        entry.layoutParams.marginStart = railRowMarginPx
        entry.layoutParams.marginEnd = railRowMarginPx
        entry.texts.forEach { it.visibility = View.GONE }
        entry.icons.forEach { icon ->
            (icon.layoutParams as? RelativeLayout.LayoutParams)?.apply {
                removeRule(RelativeLayout.ALIGN_PARENT_START)
                addRule(RelativeLayout.CENTER_HORIZONTAL)
                marginStart = 0 // don't let the landscape start-margin skew the centering
            }
        }
    }

    private fun restoreRailRow(entry: RailRow) {
        entry.layoutParams.marginStart = entry.origMarginStart
        entry.layoutParams.marginEnd = entry.origMarginEnd
        entry.texts.forEachIndexed { i, text -> text.visibility = entry.origTextVisibility[i] }
        entry.icons.forEachIndexed { i, icon ->
            (icon.layoutParams as? RelativeLayout.LayoutParams)?.apply {
                if (entry.origAlignParentStart[i]) addRule(RelativeLayout.ALIGN_PARENT_START)
                else removeRule(RelativeLayout.ALIGN_PARENT_START)
                if (entry.origCenterHorizontal[i]) addRule(RelativeLayout.CENTER_HORIZONTAL)
                else removeRule(RelativeLayout.CENTER_HORIZONTAL)
                marginStart = entry.origIconMarginStart[i]
            }
        }
    }
}
