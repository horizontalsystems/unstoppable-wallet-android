package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_input_amount.view.*

class AmountInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    var maxButtonVisible: Boolean = false
        set(value) {
            field = value
            syncButtonStates()
        }

    var onTextChangeCallback: ((text: String?) -> Unit)? = null
    var onTapMaxCallback: (() -> Unit)? = null
    var onTapSecondaryCallback: (() -> Unit)? = null

    private val textWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChangeCallback?.invoke(s?.toString())
            syncButtonStates()
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    }

    init {
        inflate(context, R.layout.view_input_amount, this)

        editTxtAmount.addTextChangedListener(textWatcher)
        editTxtAmount.requestFocus()

        btnMax.setOnClickListener {
            onTapMaxCallback?.invoke()
        }
        secondaryArea.setOnClickListener {
            onTapSecondaryCallback?.invoke()
        }
    }

    fun setAmount(text: String?, skipChangeEvent: Boolean = true) {
        editTxtAmount.apply {
            if (skipChangeEvent) {
                removeTextChangedListener(textWatcher)
            }

            setText(text)
            editTxtAmount.setSelection(text?.length ?: 0)
            syncButtonStates()

            if (skipChangeEvent) {
                addTextChangedListener(textWatcher)
            }
        }
    }

    fun setPrefix(prefix: String) {
        topAmountPrefix.text = prefix
        topAmountPrefix.isVisible = prefix.isNotBlank()
    }

    fun setSecondary(text: String){
        txtHintInfo.text = text
    }

    fun revertAmount(amount: String) {
        setAmount(amount)
        val shake = AnimationUtils.loadAnimation(context, R.anim.shake_edittext)
        editTxtAmount.startAnimation(shake)
    }

    fun setSecondaryEnabled(enabled: Boolean){
        secondaryArea.isEnabled = enabled
        context?.let { ctx ->
            val color = ctx.getColor(if (enabled) R.color.grey else R.color.grey_50)
            txtHintInfo.setTextColor(color)
        }
    }

    private fun syncButtonStates() {
        btnMax.isVisible = maxButtonVisible && editTxtAmount.text.isNullOrBlank()
    }
}
