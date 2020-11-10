package io.horizontalsystems.seekbar

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import io.horizontalsystems.views.helpers.LayoutHelper

class SeekBarConfig(context: Context) {

    val linePadding = LayoutHelper.dpToPx(22f, context).toInt()
    val bubbleWidth = LayoutHelper.dpToPx(78f, context)
    val bubbleHeight = LayoutHelper.dpToPx(52f, context)

    val notoSans: Typeface = Typeface.DEFAULT

    val primaryTextSize = LayoutHelper.spToPx(16f, context)
    val primaryTextTopMargin = LayoutHelper.dpToPx(11f, context)
    val secondaryTextSize = LayoutHelper.spToPx(10f, context)
    val strokeWidth = LayoutHelper.dpToPx(1f, context)
    val seekbarLineStrokeWidth = LayoutHelper.dpToPx(4f, context)
    val sideSymbolWidth = LayoutHelper.dpToPx(9f, context)
    val seekbarSideMargin = LayoutHelper.dpToPx(10f, context)

    var textColor = context.getColor(R.color.grey)
    var textColorSecondary = context.getColor(R.color.grey)
    var symbolColor = context.getColor(R.color.grey)
    var bubbleBackground = context.getColor(R.color.dark)
    var mainLineColor = context.getColor(R.color.steel_20)
    var bubbleStroke = context.getColor(R.color.steel_20)
    var bubbleCornerRadius = LayoutHelper.dpToPx(8f, context)

    var bubbleHint = "sat/byte"
}