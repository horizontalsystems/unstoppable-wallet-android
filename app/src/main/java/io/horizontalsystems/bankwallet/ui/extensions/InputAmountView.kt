package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.graphics.PorterDuff
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.view_amount_input.view.*
import kotlinx.android.synthetic.main.view_bottom_sheet_send.view.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

class InputAmountView : ConstraintLayout {

    private val amountChangeSubject: PublishSubject<BigDecimal> = PublishSubject.create()

    init {
        inflate(context, R.layout.view_amount_input, this)

        disposable = amountChangeSubject.debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    //delegate.onAmountChanged(it)
                }
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var decimalSize: Int? = null
    private var disposable: Disposable? = null

    override fun onDetachedFromWindow() {
        disposable?.dispose()
        super.onDetachedFromWindow()
    }

    fun bindInitial(decimalSize: Int?, onMaxClick: (() -> (Unit))? = null, onSwitchClick: (() -> (Unit))? = null) {
        this.decimalSize = decimalSize
        editTxtAmount.addTextChangedListener(textChangeListener)

        btnSwitch.visibility = View.VISIBLE
        btnMax.visibility =  View.VISIBLE

        btnMax?.setOnClickListener { onMaxClick?.invoke() }
        btnSwitch?.setOnClickListener { onSwitchClick?.invoke() }
        btnSwitch.imageTintMode = PorterDuff.Mode.SRC_IN
        invalidate()
    }

    fun updateInput(hint: String? = null, error: String? = null) {
        txtHintInfo.visibility = if (error == null) View.VISIBLE else View.GONE
        txtHintError.visibility = if (error == null) View.GONE else View.VISIBLE
        txtHintInfo.text = hint
        txtHintError.text = error
    }

    fun enableSwitchBtn(enabled: Boolean) {
        btnSwitch.isEnabled = enabled
    }

    fun updateAmountPrefix(prefix: String) {
        topAmountPrefix.text = prefix
    }

    fun setMaxBtnVisible(visible: Boolean) {
        btnMax.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private val textChangeListener = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val amountText = s?.toString() ?: ""
            var amountNumber = when {
                amountText != "" -> amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                else -> BigDecimal.ZERO
            }
            decimalSize?.let {
                if (amountNumber.scale() > it) {
                    amountNumber = amountNumber.setScale(it, RoundingMode.FLOOR)
                    val newString = amountNumber.toPlainString()
                    editTxtAmount.setText(newString)
                    editTxtAmount.setSelection(newString.length)

                    val shake = AnimationUtils.loadAnimation(context, R.anim.shake_edittext)
                    editTxtAmount.startAnimation(shake)
                }
            }

            amountInput?.setMaxBtnVisible(amountText.isEmpty())
            amountChangeSubject.onNext(amountNumber)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

}
