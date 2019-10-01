package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.SendModule
import kotlinx.android.synthetic.main.view_amount_input.*

class SendAmountFragment(
        private val wallet: Wallet,
        private val amountModuleDelegate: SendAmountModule.IAmountModuleDelegate,
        private val sendHandler:SendModule.ISendHandler
) : Fragment() {

    private lateinit var presenter: SendAmountPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_amount_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        presenter = ViewModelProviders.of(this, SendAmountModule.Factory(wallet, sendHandler))
                .get(SendAmountPresenter::class.java)
        val presenterView = presenter.view as SendAmountView
        presenter.moduleDelegate = amountModuleDelegate
        editTxtAmount.requestFocus()

        btnMax.setOnClickListener { presenter.onMaxClick() }
        btnSwitch.setOnClickListener { presenter.onSwitchClick() }

        presenterView.amountInputPrefix.observe(viewLifecycleOwner, Observer { prefix ->
            setPrefix(prefix)
        })

        presenterView.amount.observe(viewLifecycleOwner, Observer { amount ->
            setAmount(amount)
        })

        presenterView.hint.observe(viewLifecycleOwner, Observer { hint ->
            setHint(hint)
        })

        presenterView.maxButtonVisibleValue.observe(viewLifecycleOwner, Observer { visible ->
            setMaxButtonVisibility(visible)
        })

        presenterView.addTextChangeListener.observe(viewLifecycleOwner, Observer {
            enableAmountChangeListener()
        })

        presenterView.removeTextChangeListener.observe(viewLifecycleOwner, Observer {
            removeAmountChangeListener()
        })

        presenterView.revertAmount.observe(viewLifecycleOwner, Observer { amount ->
            revertAmount(amount)
        })

        presenterView.hintErrorBalance.observe(viewLifecycleOwner, Observer { hintErrorBalance ->
            setBalanceError(hintErrorBalance)
        })

        presenterView.switchButtonEnabled.observe(viewLifecycleOwner, Observer { enabled ->
            enableCurrencySwitch(enabled)
        })

        presenter.onViewDidLoad()
    }


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
            context?.getString(R.string.Send_Error_BalanceAmount, it)
        }

        txtHintError.text = errorText
    }

    private fun enableCurrencySwitch(enabled: Boolean) {
        btnSwitch.isEnabled = enabled
    }

    private val textChangeListener = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val amountText = s?.toString() ?: ""
            presenter.onAmountChange(amountText)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
}
