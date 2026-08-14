package com.mathplanet.app

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

object Ui {
    const val PURPLE = 0xFF2B78B8.toInt()
    const val SKY = 0xFF9ED8F5.toInt()
    const val NAVY = 0xFF17324D.toInt()
    const val YELLOW = 0xFFFFD84D.toInt()
    const val SELECTED_GREEN = 0xFF4FAF72.toInt()
    const val AQUA = 0xFF65D9DF.toInt()
    const val INK = 0xFF18324A.toInt()
    const val MUTED = 0xFF667C8F.toInt()
    const val CREAM = 0xFFF3F9FC.toInt()
    const val GREEN = 0xFF25966A.toInt()

    fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    fun rounded(color: Int, radiusDp: Int, context: Context, strokeColor: Int? = null): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = context.dp(radiusDp).toFloat()
            strokeColor?.let { setStroke(context.dp(1), it) }
        }
    }

    fun text(context: Context, value: String, size: Float, color: Int = INK, bold: Boolean = false): TextView {
        return TextView(context).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = false
        }
    }

}

fun View.margin(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
    layoutParams = (layoutParams ?: LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )).also { params ->
        if (params is ViewGroup.MarginLayoutParams) {
            with(Ui) {
                params.setMargins(context.dp(left), context.dp(top), context.dp(right), context.dp(bottom))
            }
        }
    }
}
