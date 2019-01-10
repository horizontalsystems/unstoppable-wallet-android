package io.horizontalsystems.bankwallet.modules.send

import android.annotation.SuppressLint
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.*
import com.google.zxing.integration.android.IntentIntegrator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.NumPadItem
import io.horizontalsystems.bankwallet.ui.extensions.NumPadItemType
import io.horizontalsystems.bankwallet.ui.extensions.NumPadItemsAdapter
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import io.horizontalsystems.bankwallet.viewHelpers.ValueFormatter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class SendBottomSheetFragment : BottomSheetDialogFragment(), NumPadItemsAdapter.Listener {

    private lateinit var viewModel: SendViewModel
    private var inputConnection: InputConnection? = null

    private var amountEditTxt: EditText? = null
    private val amountChangeSubject: PublishSubject<Double> = PublishSubject.create()

    private var coin: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        coin?.let { coin ->
            activity?.let {
                viewModel = ViewModelProviders.of(it).get(SendViewModel::class.java)
                viewModel.init(coin)
            }
        } ?: dismiss()


        val disposable = amountChangeSubject.debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    viewModel.delegate.onAmountChanged(it)
                }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val mDialog = activity?.let { BottomSheetDialog(it, R.style.BottomDialog) }
        mDialog?.setContentView(R.layout.fragment_bottom_sheet_pay)


        mDialog?.setOnShowListener(object : DialogInterface.OnShowListener {
            override fun onShow(dialog: DialogInterface?) {
                val bottomSheet = mDialog.findViewById<View>(android.support.design.R.id.design_bottom_sheet)
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
                BottomSheetBehavior.from(bottomSheet).isFitToContents = true
            }
        })


        val numpadAdapter = NumPadItemsAdapter(this, NumPadItemType.DOT)

        val numpadRecyclerView = mDialog?.findViewById<RecyclerView>(R.id.numPadItemsRecyclerView)
        val hintInfoTxt: TextView? = mDialog?.findViewById(R.id.txtHintInfo)
        val addressTxt: TextView? = mDialog?.findViewById(R.id.txtAddress)
        val addressErrorTxt: TextView? = mDialog?.findViewById(R.id.txtAddressError)
        val amountPrefixTxt: TextView? = mDialog?.findViewById(R.id.topAmountPrefix)
        val switchButton: ImageButton? = mDialog?.findViewById(R.id.btnSwitch)
        val pasteButton: Button? = mDialog?.findViewById(R.id.btnPaste)
        val scanBarcodeButton: ImageButton? = mDialog?.findViewById(R.id.btnBarcodeScan)
        val deleteAddressButton: ImageButton? = mDialog?.findViewById(R.id.btnDeleteAddress)
        val feePrimaryTxt: TextView? = mDialog?.findViewById(R.id.txtFeePrimary)
        val feeSecondaryTxt: TextView? = mDialog?.findViewById(R.id.txtFeeSecondary)
        amountEditTxt = mDialog?.findViewById(R.id.editTxtAmount)
        val sendButton: Button? = mDialog?.findViewById(R.id.btnSend)

        amountEditTxt?.showSoftInputOnFocus = false
        inputConnection = amountEditTxt?.onCreateInputConnection(EditorInfo())
        sendButton?.isEnabled = false

        switchButton?.setOnClickListener { viewModel.delegate.onSwitchClicked() }
        scanBarcodeButton?.setOnClickListener { startScanner() }
        pasteButton?.setOnClickListener { viewModel.delegate.onPasteClicked() }
        deleteAddressButton?.setOnClickListener { viewModel.delegate.onDeleteClicked() }
        sendButton?.setOnClickListener { viewModel.delegate.onSendClicked() }

        amountEditTxt?.addTextChangedListener(textChangeListener)

        numpadRecyclerView?.adapter = numpadAdapter
        numpadRecyclerView?.layoutManager = GridLayoutManager(context, 3)

        //disable BottomSheet dragging in numpad area
        numpadRecyclerView?.setOnTouchListener { _, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> {
                    false
                }
                else -> {
                    true
                }
            }
        }

        viewModel.switchButtonEnabledLiveData.observe(this, Observer { enabled ->
            enabled?.let { switchButton?.isEnabled = it }
        })

        viewModel.coinLiveData.observe(this, Observer { coin ->
            coin?.let { coinCode ->
                context?.let {
                    val coinDrawable = ContextCompat.getDrawable(it, LayoutHelper.getCoinDrawableResource(coinCode))
                    mDialog?.findViewById<ImageView>(R.id.coinImg)?.setImageDrawable(coinDrawable)
                }
                mDialog?.findViewById<TextView>(R.id.txtTitle)?.text = getString(R.string.Send_Title, coinCode)
            }
        })

        viewModel.hintInfoLiveData.observe(this, Observer { hintInfo ->

            hintInfo?.let { hint ->
                addressTxt?.resources?.getColor(if (hint is SendModule.HintInfo.ErrorInfo) R.color.red_warning else R.color.dark, null)?.let { hintInfoTxt?.setTextColor(it) }

                when (hint) {
                    is SendModule.HintInfo.Amount -> {
                        hintInfoTxt?.text = when (hint.amountInfo) {
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
                                hintInfoTxt?.text = hintInfoTxt?.context?.getString(R.string.Send_Error_BalanceAmount, balanceAmount)
                            }
                        }
                    }
                }
            }
        })

        viewModel.sendButtonEnabledLiveData.observe(this, Observer { enabled ->
            enabled?.let { sendButton?.isEnabled = it }
        })

        viewModel.amountInfoLiveData.observe(this, Observer { amountInfo ->
            var amountNumber = 0.0
            when (amountInfo) {
                is SendModule.AmountInfo.CoinValueInfo -> {
                    amountPrefixTxt?.text = amountInfo.coinValue.coinCode
                    amountNumber = Math.round(amountInfo.coinValue.value * 100_000_000.0) / 100_000_000.0
                }
                is SendModule.AmountInfo.CurrencyValueInfo -> {
                    amountPrefixTxt?.text = amountInfo.currencyValue.currency.symbol
                    amountNumber = Math.round(amountInfo.currencyValue.value * 100.0) / 100.0
                }
            }

            if (amountNumber > 0) {
                amountEditTxt?.setText(BigDecimal.valueOf(amountNumber).toPlainString())
                amountEditTxt?.setSelection(amountEditTxt?.text?.length ?: 0)
            }
        })

        viewModel.addressInfoLiveData.observe(this, Observer { addressInfo ->
            deleteAddressButton?.visibility = if (addressInfo == null) View.GONE else View.VISIBLE
            pasteButton?.visibility = if (addressInfo == null) View.VISIBLE else View.GONE
            scanBarcodeButton?.visibility = if (addressInfo == null) View.VISIBLE else View.GONE
            addressTxt?.setTextColor(addressTxt.resources.getColor(if (addressInfo == null) R.color.steel_grey else R.color.dark, null))

            if (addressInfo == null) {
                addressErrorTxt?.visibility = View.GONE
                addressTxt?.text = ""
                addressTxt?.text = addressTxt?.context?.getString(R.string.Send_Hint_Address)
            }

            addressInfo?.let {
                when (it) {
                    is SendModule.AddressInfo.ValidAddressInfo -> {
                        addressTxt?.text = it.address
                        addressErrorTxt?.visibility = View.GONE
                    }
                    is SendModule.AddressInfo.InvalidAddressInfo -> {
                        addressTxt?.text = it.address
                        addressErrorTxt?.setText(R.string.Send_Error_IncorrectAddress)
                        addressErrorTxt?.visibility = View.VISIBLE
                    }
                }
            }
        })

        viewModel.primaryFeeAmountInfoLiveData.observe(this, Observer { amountInfo ->
            amountInfo?.let {
                feePrimaryTxt?.text = when (it) {
                    is SendModule.AmountInfo.CurrencyValueInfo -> ValueFormatter.format(it.currencyValue)
                    is SendModule.AmountInfo.CoinValueInfo -> ValueFormatter.format(it.coinValue)
                }
            }
        })

        viewModel.secondaryFeeAmountInfoLiveData.observe(this, Observer { amountInfo ->
            amountInfo?.let {
                feeSecondaryTxt?.text = when (it) {
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

        viewModel.pasteButtonEnabledLiveData.observe(this, Observer { enabled ->
            enabled?.let { pasteButton?.isEnabled = it }
        })

        return mDialog as Dialog
    }

    override fun onItemClick(item: NumPadItem) {
        when (item.type) {
            NumPadItemType.NUMBER -> inputConnection?.commitText(item.number.toString(), 1)
            NumPadItemType.DELETE -> inputConnection?.deleteSurroundingText(1, 0)
            NumPadItemType.DOT -> {
                if (amountEditTxt?.text?.toString()?.contains(".") != true) {
                    inputConnection?.commitText(".", 1)
                }
            }
        }
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

            amountChangeSubject.onNext(amountNumber)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    companion object {
        fun show(activity: FragmentActivity, coin: String) {
            val fragment = SendBottomSheetFragment()
            fragment.coin = coin
            fragment.show(activity.supportFragmentManager, "pay_fragment")
        }
    }
}
