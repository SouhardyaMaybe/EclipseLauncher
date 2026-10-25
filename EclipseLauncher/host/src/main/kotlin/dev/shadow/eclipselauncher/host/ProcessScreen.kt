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
 * A deliberately plain process screen.
 *
 * The real surfaces arrive with the JNI bridge; until then the screen exists to
 * prove the process boundary, the manifest merge and the launch path. It must
 * not pretend the game or installer is working.
 */
internal fun processScreen(context: Context, title: String, body: String): View {
    val padding = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        32f,
        context.resources.displayMetrics,
    ).toInt()

    return LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(padding, padding, padding, padding)
        setBackgroundColor(Color.parseColor("#141210"))

        addView(
            TextView(context).apply {
                text = title
                setTextColor(Color.parseColor("#EDE7DF"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                gravity = Gravity.CENTER
            },
        )
        addView(
            TextView(context).apply {
                text = body
                setTextColor(Color.parseColor("#D5C4B7"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                gravity = Gravity.CENTER
                setPadding(0, padding / 2, 0, 0)
            },
        )
    }
}
