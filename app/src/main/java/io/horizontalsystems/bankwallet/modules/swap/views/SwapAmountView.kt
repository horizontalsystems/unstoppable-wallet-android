package io.horizontalsystems.bankwallet.modules.swap.views

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_amount_swap.view.*

class SwapAmountView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_amount_swap, this)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.SwapAmountView)
        try {
            maxButton.isVisible = ta.getBoolean(R.styleable.SwapAmountView_showMax, false)
        } finally {
            ta.recycle()
        }
    }

    val editText: EditText
        get() = amountText

    fun setError(text: String?) {
        error.text = text
        error.isVisible = text != null
    }

    fun setSelectedCoin(title: String) {
        coinSelectorButton.text = title
    }

    fun onMaxButtonClick(callback: () -> Unit) {
        maxButton.setOnClickListener {
            callback()
        }
    }

    fun onTokenButtonClick(callback: () -> Unit) {
        coinSelectorButton.setOnClickListener {
            callback()
        }
    }

}
