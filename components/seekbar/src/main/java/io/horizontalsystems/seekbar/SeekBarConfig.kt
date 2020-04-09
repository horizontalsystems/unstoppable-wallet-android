package io.horizontalsystems.seekbar

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import io.horizontalsystems.views.helpers.LayoutHelper

class SeekBarConfig(context: Context) {

    val linePadding = LayoutHelper.dpToPx(16f, context).toInt()
    val bubbleWidth = LayoutHelper.dpToPx(78f, context)
    val bubbleHeight = LayoutHelper.dpToPx(52f, context)

    val notoSans: Typeface = Typeface.create(ResourcesCompat.getFont(context, R.font.noto_sans_regular), Typeface.NORMAL)

    val textSize = LayoutHelper.spToPx(22f, context)
    val textSizeSecondary = LayoutHelper.spToPx(14f, context)
    val strokeWidth = LayoutHelper.dpToPx(1f, context)

    var textColor = context.getColor(R.color.grey)
    var textColorSecondary = context.getColor(R.color.grey)
    var controlsColor = context.getColor(R.color.grey)
    var bubbleBackground = context.getColor(R.color.dark)
    var bubbleStroke = context.getColor(R.color.steel_20)

    var bubbleHint = "sat/byte"
}