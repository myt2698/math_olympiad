package com.mathplanet.app

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

object Ui {
    const val PURPLE = 0xFF5B5BD6.toInt()
    const val INK = 0xFF22233D.toInt()
    const val MUTED = 0xFF77788F.toInt()
    const val CREAM = 0xFFFBFAF6.toInt()
    const val GREEN = 0xFF42B883.toInt()

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
