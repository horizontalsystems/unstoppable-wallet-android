package io.horizontalsystems.bankwallet.modules.swap.views

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
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

    fun onMaxButtonClick(callback: () -> Unit) {
        maxButton.setOnClickListener {
            callback()
        }
    }

    fun onTokenButtonClick(callback: () -> Unit) {
        tokenSelectorButton.setOnClickListener {
            callback()
        }
    }

    fun onAmountChange(callback: (String) -> Unit) {
        amountText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                callback(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
    }

}
