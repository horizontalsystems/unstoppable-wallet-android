package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import java.math.BigDecimal

class RateDiffView : androidx.appcompat.widget.AppCompatTextView {

    private var negativeColor = context.getColor(R.color.lucian)
    private var positiveColor = context.getColor(R.color.remus)

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

    fun setDiff(value: BigDecimal?) {
        if (value == null) {
            text = null
        } else {
            val sign = if (value >= BigDecimal.ZERO) "+" else "-"
            text = App.numberFormatter.format(value.abs(), 0, 2, sign, "%")

            val color = if (value >= BigDecimal.ZERO) positiveColor else negativeColor

            setTextColor(color)
        }
    }

}
