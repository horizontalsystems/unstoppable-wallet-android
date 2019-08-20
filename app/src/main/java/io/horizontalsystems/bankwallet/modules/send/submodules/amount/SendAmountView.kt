package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import android.content.Context
import android.graphics.PorterDuff
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_amount_input.view.*

class SendAmountView : ConstraintLayout {

    private var delegate: SendAmountModule.IViewDelegate? = null

    init {
        inflate(context, R.layout.view_amount_input, this)

        btnSwitch.visibility = View.VISIBLE
        btnMax.visibility = View.VISIBLE
        btnSwitch.imageTintMode = PorterDuff.Mode.SRC_IN

        invalidate()
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, lifecycleOwner: LifecycleOwner, sendAmountViewModel: SendAmountViewModel) : super(context) {

        delegate = sendAmountViewModel.delegate

        btnMax.setOnClickListener { delegate?.onMaxClick() }

        btnSwitch.setOnClickListener { delegate?.onSwitchClick() }

        delegate?.onViewDidLoad()

        sendAmountViewModel.amountInputPrefixLiveData.observe(lifecycleOwner, Observer { prefix ->
            setPrefix(prefix)
        })

        sendAmountViewModel.amountLiveData.observe(lifecycleOwner, Observer { amount ->
            setAmount(amount)
        })

        sendAmountViewModel.hintLiveData.observe(lifecycleOwner, Observer { hint ->
            setHint(hint)
        })

        sendAmountViewModel.maxButtonVisibleValueLiveData.observe(lifecycleOwner, Observer { visible ->
            setMaxButtonVisibility(visible)
        })

        sendAmountViewModel.addTextChangeListenerLiveEvent.observe(lifecycleOwner, Observer {
            enableAmountChangeListener()
        })

        sendAmountViewModel.removeTextChangeListenerLiveEvent.observe(lifecycleOwner, Observer {
            removeAmountChangeListener()
        })

        sendAmountViewModel.revertAmountLiveEvent.observe(lifecycleOwner, Observer { amount ->
            revertAmount(amount)
        })

        sendAmountViewModel.hintErrorBalanceLiveData.observe(lifecycleOwner, Observer { hintErrorBalance ->
            setBalanceError(hintErrorBalance)
        })

        sendAmountViewModel.switchButtonEnabledLiveData.observe(lifecycleOwner, Observer { enabled ->
            enableCurrencySwitch(enabled)
        })
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private fun setPrefix(prefix: String?) {
        topAmountPrefix.text = prefix
    }

    private fun setAmount(amount: String) {
        editTxtAmount.setText(amount)
        editTxtAmount.setSelection(editTxtAmount.text.length)
    }

    private fun setHint(hint: String?) {
        txtHintInfo.text = hint
    }

    private fun setMaxButtonVisibility(visible: Boolean) {
        btnMax.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun enableAmountChangeListener() {
        editTxtAmount.addTextChangedListener(textChangeListener)
    }

    private fun removeAmountChangeListener() {
        editTxtAmount.removeTextChangedListener(textChangeListener)
    }

    private fun revertAmount(amount: String) {
        editTxtAmount.setText(amount)
        editTxtAmount.setSelection(amount.length)
        val shake = AnimationUtils.loadAnimation(context, R.anim.shake_edittext)
        editTxtAmount.startAnimation(shake)
    }

    private fun setBalanceError(balanceError: String?) {
        txtHintError.visibility = if (balanceError == null) View.GONE else View.VISIBLE
        txtHintInfo.visibility = if (balanceError == null) View.VISIBLE else View.GONE

        val errorText: String? = balanceError?.let {
            context.getString(R.string.Send_Error_BalanceAmount, it)
        }

        txtHintError.text = errorText
    }

    private fun enableCurrencySwitch(enabled: Boolean) {
        btnSwitch.isEnabled = enabled
    }

    private val textChangeListener = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val amountText = s?.toString() ?: ""
            delegate?.onAmountChange(amountText)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

}
