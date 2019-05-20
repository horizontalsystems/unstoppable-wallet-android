package io.horizontalsystems.bankwallet.modules.main

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.NonNull
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager.widget.ViewPager
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.zxing.integration.android.IntentIntegrator
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.reObserve
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.receive.ReceiveViewModel
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem
import io.horizontalsystems.bankwallet.modules.send.ConfirmationFragment
import io.horizontalsystems.bankwallet.modules.send.QRScannerActivity
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendViewModel
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoViewModel
import io.horizontalsystems.bankwallet.ui.extensions.NumPadItem
import io.horizontalsystems.bankwallet.ui.extensions.NumPadItemType
import io.horizontalsystems.bankwallet.ui.extensions.NumPadItemsAdapter
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.fragment_bottom_sheet_pay.*
import kotlinx.android.synthetic.main.fragment_bottom_sheet_receive.*
import kotlinx.android.synthetic.main.transaction_info_bottom_sheet.*
import kotlinx.android.synthetic.main.view_amount_input.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit


class MainActivity : BaseActivity(), NumPadItemsAdapter.Listener {

    private lateinit var adapter: MainTabsAdapter
    private var disposable: Disposable? = null
    private lateinit var receiveViewModel: ReceiveViewModel
    private lateinit var sendViewModel: SendViewModel
    private lateinit var transInfoViewModel: TransactionInfoViewModel
    private var receiveBottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var sendBottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var txInfoBottomSheetBehavior: BottomSheetBehavior<View>? = null

    private var sendInputConnection: InputConnection? = null
    private val amountChangeSubject: PublishSubject<BigDecimal> = PublishSubject.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dashboard)

        setTopMarginByStatusBarHeight(viewPager)

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        adapter = MainTabsAdapter(supportFragmentManager)

        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 2
        setSwipeEnabled(true)

        LayoutHelper.getAttr(R.attr.BottomNavigationBackgroundColor, theme)?.let {
            bottomNavigation.defaultBackgroundColor = it
        }
        bottomNavigation.accentColor = ContextCompat.getColor(this, R.color.yellow_crypto)
        bottomNavigation.inactiveColor = ContextCompat.getColor(this, R.color.grey)

        bottomNavigation.addItem(AHBottomNavigationItem(R.string.Balance_Title, R.drawable.bank_icon, 0))
        bottomNavigation.addItem(AHBottomNavigationItem(R.string.Transactions_Title, R.drawable.transactions, 0))
        bottomNavigation.addItem(AHBottomNavigationItem(R.string.Settings_Title, R.drawable.settings, 0))
        bottomNavigation.titleState = AHBottomNavigation.TitleState.ALWAYS_HIDE
        bottomNavigation.setUseElevation(false)

        bottomNavigation.setOnTabSelectedListener { position, wasSelected ->
            if (!wasSelected) {
                viewPager.setCurrentItem(position, false)
            }
            true
        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (positionOffset == 0f) {
                    adapter.currentItem = bottomNavigation.currentItem
                }
            }

            override fun onPageSelected(position: Int) {
                bottomNavigation.currentItem = position
            }
        })

        val activeTab = intent?.extras?.getInt(ACTIVE_TAB_KEY)
        activeTab?.let {
            bottomNavigation.currentItem = it
        }

        disposable = App.appCloseManager.appCloseSignal.subscribe {
            moveTaskToBack(false)
        }

        setBottomSheets()
    }

    private fun setBottomSheets() {
        hideDim()

        receiveBottomSheetBehavior = BottomSheetBehavior.from(receiveNestedScrollView)
        sendBottomSheetBehavior = BottomSheetBehavior.from(sendNestedScrollView)
        txInfoBottomSheetBehavior = BottomSheetBehavior.from(transactionInfoNestedScrollView)

        setBottomSheet(receiveBottomSheetBehavior)
        setBottomSheet(sendBottomSheetBehavior)
        setBottomSheet(txInfoBottomSheetBehavior)

        setReceiveDialog()
        setSendDialog()
        setTransactionInfoDialog()

        bottomSheetDim.setOnClickListener {
            receiveBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            sendBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            txInfoBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onBackPressed() {
        if (adapter.currentItem == 1 && adapter.getTransactionFragment().onBackPressed()) {
            return
        } else if (adapter.currentItem > 0) {
            viewPager.currentItem = 0
            return
        }
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        sendViewModel.onViewResumed()
    }

    override fun onDestroy() {
        disposable?.dispose()
        super.onDestroy()
    }

    fun updateSettingsTabCounter(count: Int) {
        val countText = if (count < 1) "" else count.toString()
        val settingsTabPosition = 2
        bottomNavigation.setNotification(countText, settingsTabPosition)
    }

    fun setSwipeEnabled(enabled: Boolean) {
        viewPager.setPagingEnabled(enabled)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (scanResult != null && !TextUtils.isEmpty(scanResult.contents)) {
            sendViewModel.delegate.onScanAddress(scanResult.contents)
        }
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

    private fun setBottomSheet(bottomSheetBehavior: BottomSheetBehavior<View>?) {
        bottomSheetBehavior?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {
                val enabled = newState != BottomSheetBehavior.STATE_EXPANDED
                setSwipeEnabled(enabled)
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.isFitToContents = true
                }
            }

            override fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) {
                bottomSheetDim.alpha = slideOffset
                bottomSheetDim.visibility = if (slideOffset == 0f) View.GONE else View.VISIBLE
            }
        })
    }

    private fun hideDim() {
        bottomSheetDim.visibility = View.GONE
        bottomSheetDim.alpha = 0f
    }

    /***
    Receive bottomsheet START
     */

    private fun setReceiveDialog() {
        receiveViewModel = ViewModelProviders.of(this).get(ReceiveViewModel::class.java)

        receiveViewModel.showAddressLiveData.reObserve(this, showAddressObserver)
        receiveViewModel.showErrorLiveData.reObserve(this, showErrorObserver)
        receiveViewModel.showCopiedLiveEvent.reObserve(this, showCopiedObserver)
        receiveViewModel.shareAddressLiveEvent.reObserve(this, shareAddressObserver)

        btnShare.setOnClickListener { receiveViewModel.delegate.onShareClick() }
        receiveAddressView.setOnClickListener { receiveViewModel.delegate.onAddressClick() }
    }

    fun openReceiveDialog(coinCode: String) {
        receiveViewModel.init(coinCode)
        receiveBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun closeReceiveDialog() {
        receiveBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        hideDim()
    }

    //Receive dialog observers
    private val showAddressObserver = Observer<AddressItem?> { address ->
        address?.let {
            receiveCoinIcon.bind(it.coin)
            receiveTxtTitle.text = getString(R.string.Deposit_Title, it.coin.title)
            receiveAddressView.bind(it.address)
            imgQrCode.setImageBitmap(TextHelper.getQrCodeBitmapFromAddress(it.address))
        }
    }

    private val showErrorObserver = Observer<Int?> { error ->
        error?.let { HudHelper.showErrorMessage(it) }
        closeReceiveDialog()
    }

    private val showCopiedObserver = Observer<Unit> { HudHelper.showSuccessMessage(R.string.Hud_Text_Copied) }

    private val shareAddressObserver = Observer<String?> { address ->
        address?.let {
            ShareCompat.IntentBuilder.from(this)
                    .setType("text/plain")
                    .setText(it)
                    .startChooser()
        }
    }
    //End of ReceiveDialog observers

    /***
    Receive bottomsheet END
     */

    /***
    Send bottomsheet START
     */

    fun openSendDialog(coinCode: String) {
        editTxtAmount.setText("")
        sendViewModel.init(coinCode)
        sendBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

        disposable?.dispose()
        disposable = amountChangeSubject.debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    sendViewModel.delegate.onAmountChanged(it)
                }
    }

    private fun closeSendDialog() {
        sendBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        hideDim()
    }

    //SendDialog observers
    private val errorLiveDataObserver = Observer<Int?> { error -> error?.let { HudHelper.showErrorMessage(it) } }

    private val showConfirmationObserver = Observer<Unit> { ConfirmationFragment.show(this) }

    private val dismissObserver = Observer<Unit> {
        closeSendDialog()
    }

    private val dismissWithSuccessObserver = Observer<Unit> {
        HudHelper.showSuccessMessage(R.string.Send_Success)
        closeSendDialog()
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
            sendTxtTitle.text = getString(R.string.Send_Title, it.title)
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
                        getString(R.string.Send_Error_BalanceAmount, balanceAmount)
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
                    errorText = getString(R.string.Send_Error_IncorrectAddress)
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
                feeError.text = getString(R.string.Send_ERC_Alert, erc20Error.erc20CoinCode, erc20Error.coinValue.value.toPlainString())
            } ?: run {

                val primaryFeeInfo = it.primaryFeeInfo
                val secondaryFeeInfo = it.secondaryFeeInfo

                val primaryFee = when (primaryFeeInfo) {
                    is SendModule.AmountInfo.CurrencyValueInfo -> App.numberFormatter.format(primaryFeeInfo.currencyValue)
                    is SendModule.AmountInfo.CoinValueInfo -> App.numberFormatter.format(primaryFeeInfo.coinValue)
                    else -> ""
                }
                val primaryFeeText = "${getString(R.string.Send_DialogFee)} $primaryFee"
                txtFeePrimary.text = primaryFeeText

                txtFeeSecondary.text = when (secondaryFeeInfo) {
                    is SendModule.AmountInfo.CurrencyValueInfo -> App.numberFormatter.format(secondaryFeeInfo.currencyValue)
                    is SendModule.AmountInfo.CoinValueInfo -> App.numberFormatter.format(secondaryFeeInfo.coinValue)
                    else -> ""
                }
            }
        }
    }
    //Observers ends

    private fun setSendDialog() {
        sendViewModel = ViewModelProviders.of(this).get(SendViewModel::class.java)

        sendViewModel.errorLiveData.reObserve(this, errorLiveDataObserver)
        sendViewModel.showConfirmationLiveEvent.reObserve(this, showConfirmationObserver)
        sendViewModel.pasteButtonEnabledLiveData.reObserve(this, pasteButtonEnabledObserver)
        sendViewModel.feeIsAdjustableLiveData.reObserve(this, feeIsAdjustableObserver)
        sendViewModel.dismissLiveEvent.reObserve(this, dismissObserver)
        sendViewModel.dismissWithSuccessLiveEvent.reObserve(this, dismissWithSuccessObserver)
        sendViewModel.switchButtonEnabledLiveData.reObserve(this, switchButtonEnabledObserver)
        sendViewModel.coinLiveData.reObserve(this, coinLiveDataObserver)
        sendViewModel.hintInfoLiveData.reObserve(this, hintInfoObserver)
        sendViewModel.sendButtonEnabledLiveData.reObserve(this, sendButtonEnabledObserver)
        sendViewModel.amountInfoLiveData.reObserve(this, amountInfoObserver)
        sendViewModel.addressInfoLiveData.reObserve(this, addressInfoObserver)
        sendViewModel.feeInfoLiveData.reObserve(this, feeInfoObserver)

        sendInputConnection = editTxtAmount.onCreateInputConnection(EditorInfo())

        val numpadAdapter = NumPadItemsAdapter(this, NumPadItemType.DOT, false)

        numPadItemsRecyclerView?.adapter = numpadAdapter
        numPadItemsRecyclerView?.layoutManager = GridLayoutManager(this, 3)

        addressInput?.bindAddressInputInitial(
                onAmpersandClick = null,
                onBarcodeClick = { startScanner() },
                onPasteClick = { sendViewModel.delegate.onPasteClicked() },
                onDeleteClick = { sendViewModel.delegate.onDeleteClicked() }
        )

        editTxtAmount.showSoftInputOnFocus = false
        sendInputConnection = editTxtAmount.onCreateInputConnection(EditorInfo())
        btnSend.isEnabled = false

        amountInput?.bindInitial(
                onMaxClick = { sendViewModel.delegate.onMaxClicked() },
                onSwitchClick = { sendViewModel.delegate.onSwitchClicked() }
        )

        btnSend.setOnClickListener { sendViewModel.delegate.onSendClicked() }

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
            sendViewModel.delegate.onFeeSliderChange(progress)
        }
    }

    private val textChangeListener = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val amountText = s?.toString() ?: ""
            var amountNumber = when {
                amountText != "" -> amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                else -> BigDecimal.ZERO
            }
            sendViewModel.decimalSize?.let {
                if (amountNumber.scale() > it) {
                    amountNumber = amountNumber.setScale(it, RoundingMode.FLOOR)
                    val newString = amountNumber.toPlainString()
                    editTxtAmount.setText(newString)
                    editTxtAmount.setSelection(newString.length)

                    val shake = AnimationUtils.loadAnimation(applicationContext, R.anim.shake_edittext)
                    editTxtAmount.startAnimation(shake)
                }
            }

            amountInput?.setMaxBtnVisible(amountText.isEmpty())
            amountChangeSubject.onNext(amountNumber)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private fun startScanner() {
        val intentIntegrator = IntentIntegrator(this)
        intentIntegrator.captureActivity = QRScannerActivity::class.java
        intentIntegrator.setOrientationLocked(true)
        intentIntegrator.setPrompt("")
        intentIntegrator.setBeepEnabled(false)
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        intentIntegrator.initiateScan()
    }

    /***
    Send bottomsheet END
     */

    /***
    TransactionInfo bottomsheet START
     */

    private fun setTransactionInfoDialog() {
        transInfoViewModel = ViewModelProviders.of(this).get(TransactionInfoViewModel::class.java)
        transInfoViewModel.init()

        transactionIdView.setOnClickListener { transInfoViewModel.onClickTransactionId() }
        txtFullInfo.setOnClickListener { transInfoViewModel.onClickOpenFillInfo() }

        transInfoViewModel.showCopiedLiveEvent.observe(this, Observer {
            HudHelper.showSuccessMessage(R.string.Hud_Text_Copied)
        })

        transInfoViewModel.showFullInfoLiveEvent.observe(this, Observer { pair ->
            pair?.let {
                FullTransactionInfoModule.start(this, transactionHash = it.first, coin = it.second)
            }
        })

        transInfoViewModel.transactionLiveData.observe(this, Observer { txRecord ->
            txRecord?.let { txRec ->
                val txStatus = txRec.status

                txInfoCoinIcon.bind(txRec.coin)

                fiatValue.apply {
                    text = txRec.currencyValue?.let { App.numberFormatter.format(it, showNegativeSign = true, canUseLessSymbol = false) }
                    setTextColor(resources.getColor(if (txRec.incoming) R.color.green_crypto else R.color.yellow_crypto, null))
                }

                coinValue.text = App.numberFormatter.format(txRec.coinValue, explicitSign = true, realNumber = true)
                coinName.text = txRec.coin.title

                itemRate.apply {
                    txRec.rate?.let {
                        val rate = getString(R.string.Balance_RatePerCoin, App.numberFormatter.format(it, canUseLessSymbol = false), txRec.coin.code)
                        bind(title = getString(R.string.TransactionInfo_HistoricalRate), value = rate)
                    }
                    visibility = if (txRec.rate == null) View.GONE else View.VISIBLE
                }

                itemTime.bind(title = getString(R.string.TransactionInfo_Time), value = txRec.date?.let { DateHelper.getFullDateWithShortMonth(it) } ?: "")

                itemStatus.bindStatus(txStatus)

                transactionIdView.bindTransactionId(txRec.transactionHash)

                itemFrom.apply {
                    setOnClickListener { transInfoViewModel.onClickFrom() }
                    visibility = if (txRec.from.isNullOrEmpty()) View.GONE else View.VISIBLE
                    bindAddress(title = getString(R.string.TransactionInfo_From), address = txRec.from, showBottomBorder = true)
                }

                itemTo.apply {
                    setOnClickListener { transInfoViewModel.onClickTo() }
                    visibility = if (txRec.to.isNullOrEmpty()) View.GONE else View.VISIBLE
                    bindAddress(title = getString(R.string.TransactionInfo_To), address = txRec.to, showBottomBorder = true)
                }

                txInfoBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        })
    }

    fun setTransactionInfoItem(txInfoItem: TransactionViewItem) {
        transInfoViewModel.setViewItem(txInfoItem)
    }

    /***
    TransactionInfo bottomsheet END
     */

    private fun setTopMarginByStatusBarHeight(view: View) {
        val newLayoutParams = view.layoutParams as ConstraintLayout.LayoutParams
        newLayoutParams.topMargin = getStatusBarHeight()
        newLayoutParams.leftMargin = 0
        newLayoutParams.rightMargin = 0
        view.layoutParams = newLayoutParams
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }


    companion object {
        const val ACTIVE_TAB_KEY = "active_tab"
        const val SETTINGS_TAB_POSITION = 2
    }

}
