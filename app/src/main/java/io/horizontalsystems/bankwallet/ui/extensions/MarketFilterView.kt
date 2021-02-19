package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_market_filter.view.*

class MarketFilterView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        inflate(context, R.layout.view_market_filter, this)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.MarketFilterView)
        try {
            title.text = ta.getString(R.styleable.MarketFilterView_title)
            setValueColored(ta.getString(R.styleable.MarketFilterView_value))
        } finally {
            ta.recycle()
        }

    }

    fun setValueColored(v: String?, @ColorRes color: Int? = null) {
        value.text = v
        color?.let { value.setTextColor(context.getColor(it)) }
    }
}