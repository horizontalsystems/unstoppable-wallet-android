package io.horizontalsystems.bankwallet.modules.send

import android.app.AlertDialog
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import com.google.zxing.integration.android.IntentIntegrator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import io.horizontalsystems.bankwallet.viewHelpers.ValueFormatter

class SendFragment : DialogFragment() {

    private var mDialog: Dialog? = null

    private lateinit var addressTxt: TextView
    private lateinit var addressErrorTxt: TextView
    private lateinit var hintInfoTxt: TextView
    private lateinit var amountEditTxt: EditText
    private lateinit var sendButton: Button
    private lateinit var switchButton: ImageButton
    private lateinit var pasteButton: Button
    private lateinit var scanBarcodeButton: ImageButton
    private lateinit var deleteAddressButton: ImageButton
    private lateinit var amountPrefixTxt: TextView
    private lateinit var feePrimaryTxt: TextView
    private lateinit var feeSecondaryTxt: TextView

    private lateinit var viewModel: SendViewModel

    private lateinit var coin: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(SendViewModel::class.java)
            viewModel.init(coin)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = activity?.let { AlertDialog.Builder(it, R.style.BottomDialog) }

        val rootView = View.inflate(context, R.layout.fragment_bottom_sheet_pay, null) as ViewGroup

        builder?.setView(rootView)

        mDialog = builder?.create()
        mDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog?.window?.setGravity(Gravity.BOTTOM)

        hintInfoTxt = rootView.findViewById(R.id.txtHintInfo)
        addressTxt = rootView.findViewById(R.id.txtAddress)
        addressErrorTxt = rootView.findViewById(R.id.txtAddressError)
        amountPrefixTxt = rootView.findViewById(R.id.topAmountPrefix)
        switchButton = rootView.findViewById(R.id.btnSwitch)
        pasteButton = rootView.findViewById(R.id.btnPaste)
        scanBarcodeButton = rootView.findViewById(R.id.btnBarcodeScan)
        deleteAddressButton = rootView.findViewById(R.id.btnDeleteAddress)
        feePrimaryTxt = rootView.findViewById(R.id.txtFeePrimary)
        feeSecondaryTxt = rootView.findViewById(R.id.txtFeeSecondary)
        amountEditTxt = rootView.findViewById(R.id.editTxtAmount)
        sendButton = rootView.findViewById(R.id.btnSend)

        sendButton.isEnabled = false

        switchButton.setOnClickListener { viewModel.delegate.onSwitchClicked() }
        scanBarcodeButton.setOnClickListener { startScanner() }
        pasteButton.setOnClickListener { viewModel.delegate.onPasteClicked() }
        deleteAddressButton.setOnClickListener { viewModel.delegate.onDeleteClicked() }
        sendButton.setOnClickListener { viewModel.delegate.onSendClicked() }

        amountEditTxt.addTextChangedListener(textChangeListener)

        viewModel.switchButtonEnabledLiveData.observe(this, Observer { enabled ->
            enabled?.let { switchButton.isEnabled = it }
        })

        viewModel.coinLiveData.observe(this, Observer { coin ->
            coin?.let { coinCode ->
                context?.let {
                    val coinDrawable = ContextCompat.getDrawable(it, LayoutHelper.getCoinDrawableResource(coinCode))
                    rootView.findViewById<ImageView>(R.id.coinImg)?.setImageDrawable(coinDrawable)
                }
                rootView.findViewById<TextView>(R.id.txtTitle)?.text = getString(R.string.Send_Title, coinCode)
            }
        })

        viewModel.hintInfoLiveData.observe(this, Observer { hintInfo ->

            hintInfo?.let { hint ->
                hintInfoTxt.setTextColor(addressTxt.resources.getColor(if (hint is SendModule.HintInfo.ErrorInfo) R.color.red_warning else R.color.dark, null))

                when (hint) {
                    is SendModule.HintInfo.Amount -> {
                        hintInfoTxt.text = when (hint.amountInfo) {
                            is SendModule.AmountInfo.CoinValueInfo -> ValueFormatter.format(hint.amountInfo.coinValue)
                            is SendModule.AmountInfo.CurrencyValueInfo -> ValueFormatter.format(hint.amountInfo.currencyValue)
                        }
                    }
                    is SendModule.HintInfo.ErrorInfo -> {
                        when (hint.error) {
                            is SendModule.AmountError.InsufficientBalance -> {
                                val balanceAmount = when (hint.error.amountInfo) {
                                    is SendModule.AmountInfo.CoinValueInfo -> ValueFormatter.format(hint.error.amountInfo.coinValue)
                                    is SendModule.AmountInfo.CurrencyValueInfo -> ValueFormatter.format(hint.error.amountInfo.currencyValue)
                                }
                                hintInfoTxt.text = hintInfoTxt.context.getString(R.string.Send_Error_BalanceAmount, balanceAmount)
                            }
                        }
                    }
                }
            }
        })

        viewModel.sendButtonEnabledLiveData.observe(this, Observer { enabled ->
            enabled?.let { sendButton.isEnabled = it }
        })

        viewModel.amountInfoLiveData.observe(this, Observer { amountInfo ->
            amountInfo?.let {
                amountPrefixTxt.text = when (it) {
                    is SendModule.AmountInfo.CoinValueInfo -> it.coinValue.coin
                    is SendModule.AmountInfo.CurrencyValueInfo -> it.currencyValue.currency.symbol
                }

                val amountNumber = when (it) {
                    is SendModule.AmountInfo.CoinValueInfo -> it.coinValue.value
                    is SendModule.AmountInfo.CurrencyValueInfo -> it.currencyValue.value
                }

                if (amountNumber > 0) {
                    amountEditTxt.setText(amountNumber.toString())
                }
            }
        })

        viewModel.addressInfoLiveData.observe(this, Observer { addressInfo ->
            deleteAddressButton.visibility = if (addressInfo == null) View.GONE else View.VISIBLE
            pasteButton.visibility = if (addressInfo == null) View.VISIBLE else View.GONE
            scanBarcodeButton.visibility = if (addressInfo == null) View.VISIBLE else View.GONE
            addressTxt.setTextColor(addressTxt.resources.getColor(if (addressInfo == null) R.color.steel_grey else R.color.dark, null))

            if (addressInfo == null) {
                addressErrorTxt.visibility = View.GONE
                addressTxt.text = ""
                addressTxt.text = addressTxt.context.getString(R.string.Send_Hint_Address)
            }

            addressInfo?.let {
                when (it) {
                    is SendModule.AddressInfo.ValidAddressInfo -> {
                        addressTxt.text = it.address
                        addressErrorTxt.visibility = View.GONE
                    }
                    is SendModule.AddressInfo.InvalidAddressInfo -> {
                        addressTxt.text = it.address
                        addressErrorTxt.setText(R.string.Send_Error_IncorrectAddress)
                        addressErrorTxt.visibility = View.VISIBLE
                    }
                }
            }
        })

        viewModel.primaryFeeAmountInfoLiveData.observe(this, Observer { amountInfo ->
            amountInfo?.let {
                feePrimaryTxt.text = when (it) {
                    is SendModule.AmountInfo.CurrencyValueInfo -> ValueFormatter.format(it.currencyValue)
                    is SendModule.AmountInfo.CoinValueInfo -> ValueFormatter.format(it.coinValue)
                }
            }
        })

        viewModel.secondaryFeeAmountInfoLiveData.observe(this, Observer { amountInfo ->
            amountInfo?.let {
                feeSecondaryTxt.text = when (it) {
                    is SendModule.AmountInfo.CurrencyValueInfo -> ValueFormatter.format(it.currencyValue)
                    is SendModule.AmountInfo.CoinValueInfo -> ValueFormatter.format(it.coinValue)
                }
            }
        })

        viewModel.dismissWithSuccessLiveEvent.observe(this, Observer {
            dismiss()
        })

        viewModel.errorLiveData.observe(this, Observer { error ->
            error?.let {
                HudHelper.showErrorMessage(R.string.Error)
            }
        })

        viewModel.showConfirmationLiveEvent.observe(this, Observer {
            activity?.let { ConfirmationFragment.show(it) }
        })

        mDialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        return mDialog as Dialog
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent)
        if (scanResult != null && !TextUtils.isEmpty(scanResult.contents)) {
            viewModel.delegate.onScanAddress(scanResult.contents)
        }
    }

    private fun startScanner() {
        val intentIntegrator = IntentIntegrator.forSupportFragment(this)
        intentIntegrator.captureActivity = QRScannerActivity::class.java
        intentIntegrator.setOrientationLocked(true)
        intentIntegrator.setPrompt("")
        intentIntegrator.setBeepEnabled(false)
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        intentIntegrator.initiateScan()
    }

    private val textChangeListener = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val amountText = s?.toString() ?: ""
            var amountNumber = 0.0
            try {
                amountNumber = amountText.toDouble()
            } catch (e: NumberFormatException) {
                Log.e("SendFragment", "Exception", e)
            }

            viewModel.delegate.onAmountChanged(amountNumber)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    companion object {
        fun show(activity: FragmentActivity, coin: String) {
            val fragment = SendFragment()
            fragment.coin = coin
            fragment.show(activity.supportFragmentManager, "pay_fragment")
        }
    }

}
