/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.host

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The process surface for the game and installer processes.
 *
 * The real surfaces arrive with the JNI bridge; until then the screen exists to
 * prove the process boundary, the manifest merge and the launch path. It uses
 * the launcher's Mojo chrome colours and must not pretend the game or installer
 * is working.
 */
internal fun processScreen(context: Context, title: String, body: String): View {
    val padding = dp(context, 24f)
    val metrics = context.resources.displayMetrics

    return LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.parseColor("#181818"))

        addView(
            TextView(context).apply {
                text = title
                setTextColor(Color.parseColor("#FFFFFF"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(context, 16f), 0, dp(context, 16f), 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(context, 52f),
                )
                setBackgroundColor(Color.parseColor("#242424"))
            },
        )
        addView(
            View(context).apply {
                setBackgroundColor(Color.parseColor("#464646"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    maxOf(1, (metrics.density * 0.5f).toInt()),
                )
            },
        )
        addView(
            TextView(context).apply {
                text = body
                setTextColor(Color.parseColor("#B2B2B2"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER
                setPadding(padding, padding, padding, padding)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f,
                )
            },
        )
    }
}

private fun dp(context: Context, value: Float): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    value,
    context.resources.displayMetrics,
).toInt()
