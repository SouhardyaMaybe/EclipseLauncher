package me.shadow.eclipselauncher.utils.anim

import android.view.View
import me.shadow.eclipselauncher.anim.animations.Animations
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.task.Task
import me.shadow.eclipselauncher.utils.anim.ViewAnimUtils.Companion.setViewAnim

class AnimUtils {
    companion object {
        @JvmStatic
        fun setVisibilityAnim(view: View, shouldShow: Boolean) {
            setVisibilityAnim(view, shouldShow, 300, null)
        }

        @JvmStatic
        fun setVisibilityAnim(view: View, shouldShow: Boolean, listener: AnimationListener?) {
            setVisibilityAnim(view, shouldShow, 300, listener)
        }

        @JvmStatic
        fun setVisibilityAnim(view: View, shouldShow: Boolean, duration: Int) {
            setVisibilityAnim(view, shouldShow, duration, null)
        }

        @JvmStatic
        fun setVisibilityAnim(
            view: View,
            shouldShow: Boolean,
            duration: Int,
            listener: AnimationListener?
        ) {
            setVisibilityAnim(view, 0, shouldShow, duration, listener)
        }

        @JvmStatic
        fun playVisibilityAnim(view: View, visible: Boolean) {
            val targetVisibility = if (visible) View.VISIBLE else View.GONE
            if (view.visibility == targetVisibility) return

            setViewAnim(view, if (visible) Animations.FadeIn else Animations.FadeOut,
                (AllSettings.animationSpeed.getValue() * 0.7).toLong(),
                { view.visibility = View.VISIBLE },
                { view.visibility = if (visible) View.VISIBLE else View.GONE })
        }

        /**
         * Convenience helper for hide animations
         * @param view the view to operate on
         * @param startDelay the delay before starting
         * @param shouldShow true: show, false: hide
         * @param duration the duration
         * @param listener the animation listener used for the before-start and end callbacks
         */
        @JvmStatic
        fun setVisibilityAnim(
            view: View,
            startDelay: Int,
            shouldShow: Boolean,
            duration: Int,
            listener: AnimationListener?
        ) {
            listener?.onStart()

            if (shouldShow && view.visibility != View.VISIBLE) {
                fadeAnim(view, startDelay.toLong(), 0f, 1f, duration) {
                    view.visibility = View.VISIBLE
                    listener?.onEnd()
                }
            } else if (!shouldShow && view.visibility != View.GONE) {
                fadeAnim(view, startDelay.toLong(), view.alpha, 0f, duration) {
                    view.visibility = View.GONE
                    listener?.onEnd()
                }
            }
        }

        /**
         * Convenience helper for fade-out/fade-in animations
         * @param view the view to operate on
         * @param startDelay the delay before starting
         * @param begin the starting alpha
         * @param end the ending alpha
         * @param duration the duration
         * @param endAction the task to run when the animation ends
         */
        @JvmStatic
        fun fadeAnim(
            view: View,
            startDelay: Long,
            begin: Float,
            end: Float,
            duration: Int,
            endAction: Runnable?
        ) {
            if ((view.visibility != View.VISIBLE && end == 0f) || (view.visibility == View.VISIBLE && end == 1f)) {
                endAction?.let { r -> Task.runTask { r.run() }.execute() }
                return
            }
            view.visibility = View.VISIBLE
            view.alpha = begin
            view.animate()
                .alpha(end)
                .setStartDelay(startDelay)
                .setDuration(duration.toLong())
                .withEndAction(endAction)
        }
    }

    interface AnimationListener {
        fun onStart()
        fun onEnd()
    }
}
