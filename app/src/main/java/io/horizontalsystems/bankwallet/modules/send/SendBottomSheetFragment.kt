package io.horizontalsystems.bankwallet.modules.send

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.zxing.integration.android.IntentIntegrator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.extensions.*
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

class SendBottomSheetFragment : BottomSheetDialogFragment(), NumPadItemsAdapter.Listener {

    private lateinit var viewModel: SendViewModel
    private var disposable: Disposable? = null
    private var inputConnection: InputConnection? = null

    private var amountEditTxt: EditText? = null
    private var amountInput: InputAmountView? = null
    private val amountChangeSubject: PublishSubject<BigDecimal> = PublishSubject.create()

    private var coin: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        coin?.let { coin ->
            activity?.let {
                viewModel = ViewModelProviders.of(it).get(SendViewModel::class.java)
                viewModel.init(coin)
            }
        } ?: dismiss()

        disposable = amountChangeSubject.debounce(300, TimeUnit.MILLISECONDS)
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
                val bottomSheet = mDialog.findViewById<View>(R.id.design_bottom_sheet)
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
                BottomSheetBehavior.from(bottomSheet).isFitToContents = true
            }
        })

        val numpadAdapter = NumPadItemsAdapter(this, NumPadItemType.DOT, false)

        val numpadRecyclerView = mDialog?.findViewById<RecyclerView>(R.id.numPadItemsRecyclerView)
        amountInput = mDialog?.findViewById(R.id.amountInput)
        val feePrimaryTxt: TextView? = mDialog?.findViewById(R.id.txtFeePrimary)
        val feeErrorTxt: TextView? = mDialog?.findViewById(R.id.feeError)
        val feeElements: ConstraintLayout? = mDialog?.findViewById(R.id.feeElements)
        val feeSecondaryTxt: TextView? = mDialog?.findViewById(R.id.txtFeeSecondary)
        amountEditTxt = mDialog?.findViewById(R.id.editTxtAmount)
        val sendButton: Button? = mDialog?.findViewById(R.id.btnSend)
        val addressInput: InputAddressView? = mDialog?.findViewById(R.id.addressInput)
        val seekbar: SeekbarView? = mDialog?.findViewById(R.id.feeRateSeekbar)

        addressInput?.bindAddressInputInitial(
                onAmpersandClick = null,
                onBarcodeClick = { startScanner() },
                onPasteClick = { viewModel.delegate.onPasteClicked() },
                onDeleteClick = { viewModel.delegate.onDeleteClicked() }
        )

        amountEditTxt?.showSoftInputOnFocus = false
        inputConnection = amountEditTxt?.onCreateInputConnection(EditorInfo())
        sendButton?.isEnabled = false

        amountInput?.bindInitial(
                onMaxClick = { viewModel.delegate.onMaxClicked() },
                onSwitchClick = { viewModel.delegate.onSwitchClicked() }
        )

        sendButton?.setOnClickListener { viewModel.delegate.onSendClicked() }

        amountEditTxt?.addTextChangedListener(textChangeListener)

        numpadRecyclerView?.adapter = numpadAdapter
        numpadRecyclerView?.layoutManager = GridLayoutManager(context, 3)

        //disable BottomSheet dragging in numpad area
        numpadRecyclerView?.setOnTouchListener { _, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_UP -> false
                else -> true
            }
        }

        seekbar?.bind { progress ->
            viewModel.delegate.onFeeSliderChange(progress)
        }

        viewModel.switchButtonEnabledLiveData.observe(this, Observer { enabled ->
            enabled?.let {
                amountInput?.enableSwitchBtn(it)
            }
        })

        viewModel.coinLiveData.observe(this, Observer { coin ->
            coin?.let { coin1 ->
                context?.let {
                    mDialog?.findViewById<CoinIconView>(R.id.coinIcon)?.bind(coin1)
                    mDialog?.findViewById<TextView>(R.id.txtTitle)?.text = getString(R.string.Send_Title, coin1.title)
                }
            }
        })

        viewModel.hintInfoLiveData.observe(this, Observer { hintInfo ->
            when (hintInfo) {
                is SendModule.HintInfo.Amount -> {
                    val hintText = when (hintInfo.amountInfo) {
                        is SendModule.AmountInfo.CoinValueInfo -> App.numberFormatter.format(hintInfo.amountInfo.coinValue, realNumber = true)
                        is SendModule.AmountInfo.CurrencyValueInfo -> App.numberFormatter.format(hintInfo.amountInfo.currencyValue, realNumber = true)
                    }
                    amountInput?.updateInput(hint = hintText)
                }
                is SendModule.HintInfo.ErrorInfo -> {
                    val errorText: String? = when (hintInfo.error) {
                        is SendModule.AmountError.InsufficientBalance -> {
                            val balanceAmount = when (hintInfo.error.amountInfo) {
                                is SendModule.AmountInfo.CoinValueInfo -> App.numberFormatter.format(hintInfo.error.amountInfo.coinValue)
                                is SendModule.AmountInfo.CurrencyValueInfo -> App.numberFormatter.format(hintInfo.error.amountInfo.currencyValue, realNumber = true)
                            }
                            context?.getString(R.string.Send_Error_BalanceAmount, balanceAmount)
                        }
                        else -> null
                    }
                    amountInput?.updateInput(error = errorText)
                }
                null -> amountInput?.updateInput()
            }
        })

        viewModel.sendButtonEnabledLiveData.observe(this, Observer { enabled ->
            enabled?.let { sendButton?.isEnabled = it }
        })

        viewModel.amountInfoLiveData.observe(this, Observer { amountInfo ->
            var amountNumber = BigDecimal.ZERO
            when (amountInfo) {
                is SendModule.AmountInfo.CoinValueInfo -> {
                    amountInput?.updateAmountPrefix(amountInfo.coinValue.coinCode)
                    amountNumber = amountInfo.coinValue.value.setScale(8, RoundingMode.HALF_EVEN)
                }
                is SendModule.AmountInfo.CurrencyValueInfo -> {
                    amountInput?.updateAmountPrefix(amountInfo.currencyValue.currency.symbol)
                    amountNumber = amountInfo.currencyValue.value.setScale(2, RoundingMode.HALF_EVEN)
                }
            }

            if (amountNumber > BigDecimal.ZERO) {
                amountEditTxt?.setText(amountNumber.stripTrailingZeros().toPlainString())
                amountEditTxt?.setSelection(amountEditTxt?.text?.length ?: 0)
            }
        })

        viewModel.addressInfoLiveData.observe(this, Observer { addressInfo ->

            var addressText = ""
            var errorText: String? = null

            addressInfo?.let {
                when (it) {
                    is SendModule.AddressInfo.ValidAddressInfo -> {
                        addressText = it.address
                    }
                    is SendModule.AddressInfo.InvalidAddressInfo -> {
                        addressText = it.address
                        errorText = context?.getString(R.string.Send_Error_IncorrectAddress)
                    }
                }
            }

            addressInput?.updateInput(addressText, errorText)
        })

        viewModel.feeInfoLiveData.observe(this, Observer { feeInfo ->
            feeInfo?.let {
                feePrimaryTxt?.visibility = if (it.error == null) View.VISIBLE else View.GONE
                feeSecondaryTxt?.visibility = if (it.error == null) View.VISIBLE else View.GONE
                feeErrorTxt?.visibility = if (it.error == null) View.GONE else View.VISIBLE
                feeElements?.visibility = if (it.error == null) View.VISIBLE else View.GONE

                it.error?.let { erc20Error ->
                    feeErrorTxt?.text = getString(R.string.Send_ERC_Alert, erc20Error.erc20CoinCode, erc20Error.coinValue.value.toPlainString())
                } ?: run {

                    val primaryFeeInfo = it.primaryFeeInfo
                    val secondaryFeeInfo = it.secondaryFeeInfo

                    val primaryFee = when (primaryFeeInfo) {
                        is SendModule.AmountInfo.CurrencyValueInfo -> App.numberFormatter.format(primaryFeeInfo.currencyValue, realNumber = true)
                        is SendModule.AmountInfo.CoinValueInfo -> App.numberFormatter.format(primaryFeeInfo.coinValue)
                        else -> ""
                    }
                    val primaryFeeText = "${getString(R.string.Send_DialogFee)} $primaryFee"
                    feePrimaryTxt?.text = primaryFeeText

                    feeSecondaryTxt?.text = when (secondaryFeeInfo) {
                        is SendModule.AmountInfo.CurrencyValueInfo -> App.numberFormatter.format(secondaryFeeInfo.currencyValue, realNumber = true)
                        is SendModule.AmountInfo.CoinValueInfo -> App.numberFormatter.format(secondaryFeeInfo.coinValue)
                        else -> ""
                    }
                }

            }
        })

        viewModel.dismissWithSuccessLiveEvent.observe(this, Observer {
            HudHelper.showSuccessMessage(R.string.Send_Success)
            dismiss()
        })

        viewModel.errorLiveData.observe(this, Observer { error ->
            error?.let {
                HudHelper.showErrorMessage(it)
            }
        })

        viewModel.showConfirmationLiveEvent.observe(this, Observer {
            activity?.let { ConfirmationFragment.show(it) }
        })

        viewModel.pasteButtonEnabledLiveData.observe(this, Observer { enabled ->
            enabled?.let { addressInput?.enablePasteButton(it) }
        })

        viewModel.feeIsAdjustableLiveData.observe(this, Observer { feeIsAdjustable ->
            feeIsAdjustable?.let {
                seekbar?.visibility = if (it) View.VISIBLE else View.GONE
            }
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

    override fun onDestroy() {
        disposable?.dispose()
        super.onDestroy()
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
            var amountNumber = when {
                amountText != "" -> amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                else -> BigDecimal.ZERO
            }
            viewModel.decimalSize?.let {
                if (amountNumber.scale() > it) {
                    amountNumber = amountNumber.setScale(it, RoundingMode.FLOOR)
                    val newString = amountNumber.toPlainString()
                    amountEditTxt?.setText(newString)
                    amountEditTxt?.setSelection(newString.length)

                    val shake = AnimationUtils.loadAnimation(context, R.anim.shake_edittext)
                    amountEditTxt?.startAnimation(shake)
                }
            }

            amountInput?.setMaxBtnVisible(amountText.isEmpty())
            amountChangeSubject.onNext(amountNumber)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    companion object {
        fun show(activity: androidx.fragment.app.FragmentActivity, coin: String) {
            val fragment = SendBottomSheetFragment()
            fragment.coin = coin
            fragment.show(activity.supportFragmentManager, "pay_fragment")
        }
    }
}
