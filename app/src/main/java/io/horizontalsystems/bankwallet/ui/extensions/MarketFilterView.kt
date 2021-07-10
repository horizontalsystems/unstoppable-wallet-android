package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import io.horizontalsystems.bankwallet.R

class MarketFilterView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {

    private var value: TextView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        val rootView = inflate(context, R.layout.view_market_filter, this)
        value = rootView.findViewById(R.id.value)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.MarketFilterView)
        try {
            rootView.findViewById<TextView>(R.id.title).text = ta.getString(R.styleable.MarketFilterView_title)
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
