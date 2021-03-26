package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.core.helpers.KeyboardHelper
import kotlinx.android.synthetic.main.view_input_amount.view.*

class AmountInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    var maxButtonVisible: Boolean = false
        set(value) {
            field = value
            syncButtonStates()
        }

    var onTextChangeCallback: ((prevText: String?, newText: String?) -> Unit)? = null
    var onTapMaxCallback: (() -> Unit)? = null
    var onTapSecondaryCallback: (() -> Unit)? = null

    private val textWatcher = object : TextWatcher {
        private var prevValue: String? = null

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChangeCallback?.invoke(prevValue, s?.toString())
            syncButtonStates()
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            prevValue = s?.toString()
        }
    }

    init {
        inflate(context, R.layout.view_input_amount, this)

        editTxtAmount.addTextChangedListener(textWatcher)

        btnMax.setOnClickListener {
            onTapMaxCallback?.invoke()
        }
        secondaryArea.setOnClickListener {
            onTapSecondaryCallback?.invoke()
        }
    }

    fun getAmount(): String? {
        return editTxtAmount.text?.toString()
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

    fun setPrefix(prefix: String?) {
        topAmountPrefix.text = prefix
        topAmountPrefix.isVisible = prefix?.isNotBlank() ?: false
    }

    fun setSecondaryText(text: String?) {
        txtHintInfo.text = text
    }

    fun setFocus() {
        KeyboardHelper.showKeyboard(context, editTxtAmount)
    }

    fun revertAmount(amount: String?) {
        amount ?: return

        setAmount(amount)
        val shake = AnimationUtils.loadAnimation(context, R.anim.shake_edittext)
        editTxtAmount.startAnimation(shake)
    }

    fun setSecondaryEnabled(enabled: Boolean) {
        secondaryArea.isEnabled = enabled
        context?.let { ctx ->
            val color = if (enabled) R.color.bran else R.color.grey_50
            txtHintInfo.setTextColor(ctx.getColor(color))
        }
    }

    private fun syncButtonStates() {
        btnMax.isVisible = maxButtonVisible && editTxtAmount.text.isNullOrBlank()
    }
}
