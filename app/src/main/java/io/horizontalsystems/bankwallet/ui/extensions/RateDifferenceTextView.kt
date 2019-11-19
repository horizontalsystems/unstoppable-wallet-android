package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.TextView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import java.math.BigDecimal
import java.math.RoundingMode

class RateDifferenceTextView : TextView {

    private val diffScale = 2

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(value: BigDecimal, context: Context, colorfulText: Boolean) {
        val scaledValue = value.setScale(diffScale, RoundingMode.HALF_EVEN).stripTrailingZeros()

        if (colorfulText) {
            val colorAttr = if (scaledValue >= BigDecimal.ZERO) R.attr.ColorRemus else R.attr.ColorLucian
            LayoutHelper.getAttr(colorAttr, context.theme)?.let { color ->
                setTextColor(color)
            }
        } else {
            setTextColor(resources.getColor(R.color.grey, context.theme))
        }

        text = "${scaledValue.abs().toPlainString()}%"

        val iconRes = if (scaledValue >= BigDecimal.ZERO) R.drawable.ic_up_green else R.drawable.ic_down_red
        setLeftIcon(context.getDrawable(iconRes))

    }

    private fun setLeftIcon(icon: Drawable?) {
        setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)
        val padding = LayoutHelper.dp(4f, context)
        compoundDrawablePadding = padding
    }
}
