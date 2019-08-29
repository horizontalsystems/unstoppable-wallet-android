package io.horizontalsystems.bankwallet.lib.chartview.models

import android.content.Context
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.lib.chartview.ViewHelper

class ChartConfig(context: Context, viewHelper: ViewHelper) {
    var showGrid = true
    var animated = true
    //  colors
    var curveColor = context.getColor(R.color.red_warning)
    var touchColor = context.getColor(R.color.bars_color)
    var gridColor = context.getColor(R.color.steel_20)
    var textColor = context.getColor(R.color.grey)
    var growColor = context.getColor(R.color.green_crypto)
    var fallColor = context.getColor(R.color.red_warning)
    var indicatorColor = context.getColor(R.color.bars_color)

    //  dimens
    var width = 0f
    var height = 0f
    var textSize = viewHelper.dp2px(12f)
    var textPadding = viewHelper.dp2px(4f)
    var strokeWidth = viewHelper.dp2px(0.5f)
    var offsetRight = 0f
    var offsetBottom = 0f

    //  grid dimens
    var valueTop = 0f
    var valueStep = 0f
    var valuePrecision = 0

    //  Animation
    var animatedFraction = 0f
}
