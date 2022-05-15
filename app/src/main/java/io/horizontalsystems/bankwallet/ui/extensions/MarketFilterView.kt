package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.databinding.ViewMarketFilterBinding

class MarketFilterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewMarketFilterBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        val ta = context.obtainStyledAttributes(attrs, R.styleable.MarketFilterView)
        try {
            binding.title.text = ta.getString(R.styleable.MarketFilterView_title)
            setValueColored(ta.getString(R.styleable.MarketFilterView_value))
        } finally {
            ta.recycle()
        }

    }

    fun setValueColored(v: String?, @ColorRes color: Int? = null) {
        binding.value.text = v
        color?.let { binding.value.setTextColor(context.getColor(it)) }
    }
}
