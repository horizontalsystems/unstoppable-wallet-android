package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.views.helpers.LayoutHelper
import java.math.BigDecimal
import java.math.RoundingMode

class RateDiffView : androidx.appcompat.widget.AppCompatTextView {

    var diff: BigDecimal? = null
        set(value) {
            field = value
            updateText(value)
        }

    private var negativeColor = context.getColor(R.color.grey)
    private var positiveColor = context.getColor(R.color.grey)
    private val diffScale = 2

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialize(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize(attrs)
    }

    private fun initialize(attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.RateDiffView)
        try {
            positiveColor = ta.getInt(R.styleable.RateDiffView_positiveColor, positiveColor)
            negativeColor = ta.getInt(R.styleable.RateDiffView_negativeColor, negativeColor)
        } finally {
            ta.recycle()
        }
    }

    private fun updateText(value: BigDecimal?) {
        if (value == null) {
            setLeftIcon(null)
            text = null
        } else {
            val scaledValue = value.setScale(diffScale, RoundingMode.HALF_EVEN).stripTrailingZeros()
            val isPositive = scaledValue >= BigDecimal.ZERO

            val textColor = if (isPositive) positiveColor else negativeColor
            val iconRes = if (isPositive) R.drawable.ic_up_green else R.drawable.ic_down_red

            setTextColor(textColor)
            setLeftIcon(context.getDrawable(iconRes))

            text = App.numberFormatter.format(scaledValue.abs(), 0, diffScale) + "%"
        }
    }

    private fun setLeftIcon(icon: Drawable?) {
        setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)
        val padding = LayoutHelper.dp(4f, context)
        compoundDrawablePadding = padding
    }
}
