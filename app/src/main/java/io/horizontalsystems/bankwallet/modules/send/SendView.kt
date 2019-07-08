package io.horizontalsystems.bankwallet.modules.send

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.reObserve
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.ui.extensions.NumPadItem
import io.horizontalsystems.bankwallet.ui.extensions.NumPadItemType
import io.horizontalsystems.bankwallet.ui.extensions.NumPadItemsAdapter
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.view_amount_input.view.*
import kotlinx.android.synthetic.main.view_bottom_sheet_send.view.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

class SendView: LinearLayout, NumPadItemsAdapter.Listener {

    private lateinit var viewModel: SendViewModel
    private lateinit var lifecycleOwner: LifecycleOwner
    private var sendInputConnection: InputConnection? = null
    private var disposable: Disposable? = null
    private val amountChangeSubject: PublishSubject<BigDecimal> = PublishSubject.create()
    private var listener: Listener? = null

    interface Listener {
        fun closeSend()
        fun openSendScanner()
        fun showSendConfirmationDialog()
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun initView(viewModel: SendViewModel, lifecycleOwner: LifecycleOwner, listener: Listener){
        inflate(context, R.layout.view_bottom_sheet_send, this)

        this.listener = listener
        this.viewModel = viewModel
        this.lifecycleOwner = lifecycleOwner

        setSendDialog()

        disposable = amountChangeSubject.debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    this.viewModel.delegate.onAmountChanged(it)
                }
    }

    fun update() {
        editTxtAmount.setText("")
        addressInput?.updateInput("")
        feeRateSeekbar.reset()
    }

    override fun onItemClick(item: NumPadItem) {
        when (item.type) {
            NumPadItemType.NUMBER -> sendInputConnection?.commitText(item.number.toString(), 1)
            NumPadItemType.DELETE -> sendInputConnection?.deleteSurroundingText(1, 0)
            NumPadItemType.DOT -> {
                if (editTxtAmount?.text?.toString()?.contains(".") != true) {
                    sendInputConnection?.commitText(".", 1)
                }
            }
        }
    }

    private fun setSendDialog() {
        viewModel.errorLiveData.reObserve(lifecycleOwner, errorLiveDataObserver)
        viewModel.showConfirmationLiveEvent.reObserve(lifecycleOwner, showConfirmationObserver)
        viewModel.pasteButtonEnabledLiveData.reObserve(lifecycleOwner, pasteButtonEnabledObserver)
        viewModel.feeIsAdjustableLiveData.reObserve(lifecycleOwner, feeIsAdjustableObserver)
        viewModel.dismissLiveEvent.reObserve(lifecycleOwner, dismissObserver)
        viewModel.dismissWithSuccessLiveEvent.reObserve(lifecycleOwner, dismissWithSuccessObserver)
        viewModel.switchButtonEnabledLiveData.reObserve(lifecycleOwner, switchButtonEnabledObserver)
        viewModel.coinLiveData.reObserve(lifecycleOwner, coinLiveDataObserver)
        viewModel.hintInfoLiveData.reObserve(lifecycleOwner, hintInfoObserver)
        viewModel.sendButtonEnabledLiveData.reObserve(lifecycleOwner, sendButtonEnabledObserver)
        viewModel.amountInfoLiveData.reObserve(lifecycleOwner, amountInfoObserver)
        viewModel.addressInfoLiveData.reObserve(lifecycleOwner, addressInfoObserver)
        viewModel.feeInfoLiveData.reObserve(lifecycleOwner, feeInfoObserver)

        sendInputConnection = editTxtAmount.onCreateInputConnection(EditorInfo())

        val numpadAdapter = NumPadItemsAdapter(this, NumPadItemType.DOT, false)

        numPadItemsRecyclerView?.adapter = numpadAdapter
        numPadItemsRecyclerView?.layoutManager = GridLayoutManager(context, 3)

        addressInput?.bindAddressInputInitial(
                onAmpersandClick = null,
                onBarcodeClick = {
                    listener?.openSendScanner()
                },
                onPasteClick = { viewModel.delegate.onPasteClicked() },
                onDeleteClick = { viewModel.delegate.onDeleteClicked() }
        )

        editTxtAmount.showSoftInputOnFocus = false
        btnSend.isEnabled = false

        amountInput?.bindInitial(
                onMaxClick = { viewModel.delegate.onMaxClicked() },
                onSwitchClick = { viewModel.delegate.onSwitchClicked() }
        )

        btnSend.setOnClickListener { viewModel.delegate.onSendClicked() }

        editTxtAmount.addTextChangedListener(textChangeListener)

        //disables BottomSheet dragging in numpad area
        numPadItemsRecyclerView?.setOnTouchListener { _, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_UP -> false
                else -> true
            }
        }

        feeRateSeekbar.bind { progress ->
            viewModel.delegate.onFeeSliderChange(progress)
        }
    }

    private val errorLiveDataObserver = Observer<Int?> { error -> error?.let { HudHelper.showErrorMessage(it) } }

    private val showConfirmationObserver = Observer<Unit> {
        listener?.showSendConfirmationDialog()
    }

    private val dismissObserver = Observer<Unit> {
        listener?.closeSend()
    }

    private val dismissWithSuccessObserver = Observer<Unit> {
        HudHelper.showSuccessMessage(R.string.Send_Success)
        listener?.closeSend()
    }

    private val pasteButtonEnabledObserver = Observer<Boolean?> { enabled ->
        enabled?.let { addressInput.enablePasteButton(it) }
    }

    private val feeIsAdjustableObserver = Observer<Boolean?> { feeIsAdjustable ->
        feeIsAdjustable?.let { feeRateSeekbar.visibility = if (it) View.VISIBLE else View.GONE }
    }

    private val switchButtonEnabledObserver = Observer<Boolean?> { enabled ->
        enabled?.let { amountInput?.enableSwitchBtn(it) }
    }

    private val coinLiveDataObserver = Observer<Coin?> { coin ->
        coin?.let {
            sendCoinIcon.bind(it)
            sendTxtTitle.text = context.getString(R.string.Send_Title, it.title)
        }
    }

    private val sendButtonEnabledObserver = Observer<Boolean?> { enabled ->
        enabled?.let { btnSend.isEnabled = it }
    }

    private val hintInfoObserver = Observer<SendModule.HintInfo> { hintInfo ->
        when (hintInfo) {
            is SendModule.HintInfo.Amount -> {
                val hintText = when (hintInfo.amountInfo) {
                    is SendModule.AmountInfo.CoinValueInfo -> App.numberFormatter.format(hintInfo.amountInfo.coinValue, realNumber = true)
                    is SendModule.AmountInfo.CurrencyValueInfo -> App.numberFormatter.format(hintInfo.amountInfo.currencyValue)
                }
                amountInput?.updateInput(hint = hintText)
            }
            is SendModule.HintInfo.ErrorInfo -> {
                val errorText: String? = when (hintInfo.error) {
                    is SendModule.AmountError.InsufficientBalance -> {
                        val balanceAmount = when (hintInfo.error.amountInfo) {
                            is SendModule.AmountInfo.CoinValueInfo -> App.numberFormatter.format(hintInfo.error.amountInfo.coinValue)
                            is SendModule.AmountInfo.CurrencyValueInfo -> App.numberFormatter.format(hintInfo.error.amountInfo.currencyValue)
                        }
                        context.getString(R.string.Send_Error_BalanceAmount, balanceAmount)
                    }
                    else -> null
                }
                amountInput?.updateInput(error = errorText)
            }
            null -> amountInput?.updateInput()
        }
    }

    private val amountInfoObserver = Observer<SendModule.AmountInfo> { amountInfo ->
        val amountNumber = when (amountInfo) {
            is SendModule.AmountInfo.CoinValueInfo -> {
                amountInput?.updateAmountPrefix(amountInfo.coinValue.coinCode)
                amountInfo.coinValue.value.setScale(8, RoundingMode.HALF_EVEN)
            }
            is SendModule.AmountInfo.CurrencyValueInfo -> {
                amountInput?.updateAmountPrefix(amountInfo.currencyValue.currency.symbol)
                amountInfo.currencyValue.value.setScale(2, RoundingMode.HALF_EVEN)
            }
            else -> BigDecimal.ZERO
        }

        if (amountNumber > BigDecimal.ZERO) {
            editTxtAmount.setText(amountNumber.stripTrailingZeros().toPlainString())
            editTxtAmount.setSelection(editTxtAmount.text.length)
        } else {
            editTxtAmount.setText("")
        }
    }

    private val addressInfoObserver = Observer<SendModule.AddressInfo> { addressInfo ->
        var addressText = ""
        var errorText: String? = null

        addressInfo?.let {
            when (it) {
                is SendModule.AddressInfo.ValidAddressInfo -> {
                    addressText = it.address
                }
                is SendModule.AddressInfo.InvalidAddressInfo -> {
                    addressText = it.address
                    errorText = context.getString(R.string.Send_Error_IncorrectAddress)
                }
            }
        }

        addressInput.updateInput(addressText, errorText)
    }

    private val feeInfoObserver = Observer<SendModule.FeeInfo> { feeInfo ->
        feeInfo?.let {
            txtFeePrimary.visibility = if (it.error == null) View.VISIBLE else View.GONE
            txtFeeSecondary.visibility = if (it.error == null) View.VISIBLE else View.GONE
            feeError.visibility = if (it.error == null) View.GONE else View.VISIBLE
            feeAmountWrapper.visibility = if (it.error == null) View.VISIBLE else View.GONE
            feeRateSeekbar.visibility = if (it.error == null) View.VISIBLE else View.GONE

            it.error?.let { erc20Error ->
                feeError.text = context.getString(R.string.Send_ERC_Alert, erc20Error.erc20CoinCode, erc20Error.coinValue.value.toPlainString())
            } ?: run {

                val primaryFeeInfo = it.primaryFeeInfo
                val secondaryFeeInfo = it.secondaryFeeInfo

                val primaryFee = when (primaryFeeInfo) {
                    is SendModule.AmountInfo.CurrencyValueInfo -> App.numberFormatter.format(primaryFeeInfo.currencyValue)
                    is SendModule.AmountInfo.CoinValueInfo -> App.numberFormatter.format(primaryFeeInfo.coinValue)
                    else -> ""
                }
                val primaryFeeText = "${context.getString(R.string.Send_DialogFee)} $primaryFee"
                txtFeePrimary.text = primaryFeeText

                txtFeeSecondary.text = when (secondaryFeeInfo) {
                    is SendModule.AmountInfo.CurrencyValueInfo -> App.numberFormatter.format(secondaryFeeInfo.currencyValue)
                    is SendModule.AmountInfo.CoinValueInfo -> App.numberFormatter.format(secondaryFeeInfo.coinValue)
                    else -> ""
                }
            }
        }
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
