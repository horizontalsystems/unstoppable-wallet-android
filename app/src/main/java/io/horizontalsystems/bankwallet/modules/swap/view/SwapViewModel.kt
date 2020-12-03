package io.horizontalsystems.bankwallet.modules.swap.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap.DataState
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.ISwapService
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.SwapError
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.SwapState
import io.horizontalsystems.bankwallet.modules.swap.confirmation.ConfirmationPresenter
import io.horizontalsystems.bankwallet.modules.swap.model.AmountType
import io.horizontalsystems.bankwallet.modules.swap.model.Trade
import io.horizontalsystems.bankwallet.modules.swap.provider.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.SwapSettings
import io.horizontalsystems.bankwallet.modules.swap.view.item.TradeViewItem
import io.horizontalsystems.bankwallet.modules.swap_new.SwapViewItemHelper
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SwapViewModel(
        val confirmationPresenter: ConfirmationPresenter,
        private val swapService: ISwapService,
        private val stringProvider: StringProvider,
        private val formatter: SwapViewItemHelper,
        private val clearables: List<Clearable>
) : ViewModel() {

    private val disposables = CompositeDisposable()

    // region Outputs
    private val _coinSending = MutableLiveData<Coin>()
    val coinSending: LiveData<Coin?> = _coinSending

    private val _coinReceiving = MutableLiveData<Coin>()
    val coinReceiving: LiveData<Coin?> = _coinReceiving

    private val _allowance = MutableLiveData<String?>()
    val allowance: LiveData<String?> = _allowance

    private val _allowanceLoading = MutableLiveData<Boolean>()
    val allowanceLoading: LiveData<Boolean> = _allowanceLoading

    private val _balanceSending = MutableLiveData<String?>()
    val balanceSending: LiveData<String?> = _balanceSending

    private val _balanceReceiving = MutableLiveData<String?>()
    val balanceReceiving: LiveData<String?> = _balanceReceiving

    private val _amountSending = MutableLiveData<String?>()
    val amountSending: LiveData<String?> = _amountSending

    private val _amountReceiving = MutableLiveData<String?>()
    val amountReceiving: LiveData<String?> = _amountReceiving

    private val _amountSendingEstimated = MutableLiveData<Boolean>()
    val amountSendingEstimated: LiveData<Boolean> = _amountSendingEstimated

    private val _amountReceivingEstimated = MutableLiveData<Boolean>()
    val amountReceivingEstimated: LiveData<Boolean> = _amountReceivingEstimated

    private val _tradeViewItem = MutableLiveData<TradeViewItem?>()
    val tradeViewItem: LiveData<TradeViewItem?> = _tradeViewItem

    private val _insufficientAllowance = MutableLiveData<Boolean>()
    val insufficientAllowance: LiveData<Boolean> = _insufficientAllowance

    private val _amountSendingError = MutableLiveData<String?>()
    val amountSendingError: LiveData<String?> = _amountSendingError

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _approveData = MutableLiveData<SwapModule.ApproveData?>()
    val approveData: LiveData<SwapModule.ApproveData?> = _approveData

    private val _showApprovingMessage = MutableLiveData<Boolean>()
    val showApprovingMessage: LiveData<Boolean> = _showApprovingMessage

    private val _proceedButtonEnabled = MutableLiveData<Boolean>()
    val proceedButtonEnabled: LiveData<Boolean> = _proceedButtonEnabled

    private val _openConfirmation = SingleLiveEvent<Boolean>()
    val openConfirmation: LiveData<Boolean> = _openConfirmation

    private val _openSettings = SingleLiveEvent<Pair<SwapSettings, SwapSettings>>()
    val openSettings: LiveData<Pair<SwapSettings, SwapSettings>> = _openSettings

    private val _closeWithSuccess = SingleLiveEvent<Int>()
    val closeWithSuccess: LiveData<Int> = _closeWithSuccess

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private var feeLoading = false
        set(value) {
            field = value
            updateLoading()
        }

    private var approving = false
        set(value) {
            field = value
            _showApprovingMessage.postValue(value)
            updateLoading()
        }

    private var tradeItemLoading = false
        set(value) {
            field = value
            updateLoading()
        }

    // endregion

    init {
        swapService.coinSendingObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    _coinSending.postValue(it.orElse(null))
                }
                .let { disposables.add(it) }

        swapService.coinReceivingObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    _coinReceiving.postValue(it.orElse(null))
                }
                .let { disposables.add(it) }

        swapService.balanceSending
                .subscribeOn(Schedulers.io())
                .subscribe { optionalBalance ->
                    val balance = if (optionalBalance.isPresent) optionalBalance.get() else null
                    _balanceSending.postValue(balance?.let { formatter.coinAmount(it.value, it.coin) })
                }
                .let { disposables.add(it) }

        swapService.balanceReceiving
                .subscribeOn(Schedulers.io())
                .subscribe { optionalBalance ->
                    val balance = if (optionalBalance.isPresent) optionalBalance.get() else null
                    _balanceReceiving.postValue(balance?.let { formatter.coinAmount(it.value, it.coin) })
                }
                .let { disposables.add(it) }

        swapService.amountReceivingObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    _amountReceiving.postValue(if (it.isPresent) it.get().toPlainString() else null)
                }
                .let { disposables.add(it) }

        swapService.amountSendingObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    _amountSending.postValue(if (it.isPresent) it.get().toPlainString() else null)
                }
                .let { disposables.add(it) }

        swapService.allowance
                .subscribeOn(Schedulers.io())
                .subscribe {
                    if (it is DataState.Success) {
                        _allowance.postValue(it.data?.let { coinValue ->
                            formatter.coinAmount(coinValue.value, coinValue.coin)
                        })
                    }
                    _allowanceLoading.postValue(it is DataState.Loading)
                }
                .let { disposables.add(it) }

        swapService.tradeObservable
                .subscribeOn(Schedulers.io())
                .subscribe { dataState ->
                    if (dataState is DataState.Success || dataState is DataState.Error) {
                        _tradeViewItem.postValue(dataState.dataOrNull?.let { tradeViewItem(it) })
                    }
                    tradeItemLoading = dataState is DataState.Loading
                }
                .let { disposables.add(it) }

        swapService.errors
                .subscribeOn(Schedulers.io())
                .subscribe { errors ->
                    val insufficientBalanceError = errors.firstOrNull { it == SwapError.InsufficientBalance }
                    _amountSendingError.postValue(insufficientBalanceError?.let { stringProvider.string(R.string.Swap_ErrorInsufficientBalance) })

                    val insufficientAllowanceError = errors.firstOrNull { it is SwapError.InsufficientAllowance }
                    _insufficientAllowance.postValue(insufficientAllowanceError != null)

                    _error.postValue(errorText(errors))
                }
                .let { disposables.add(it) }

        swapService.amountType
                .subscribeOn(Schedulers.io())
                .subscribe {
                    _amountSendingEstimated.postValue(it == AmountType.ExactReceiving)
                    _amountReceivingEstimated.postValue(it == AmountType.ExactSending)
                }
                .let { disposables.add(it) }

        swapService.state
                .subscribeOn(Schedulers.io())
                .subscribe {
                    approving = it is SwapState.WaitingForApprove
                    _approveData.postValue((it as? SwapState.ApproveRequired)?.data)

                    _proceedButtonEnabled.postValue(it == SwapState.ProceedAllowed)

                    feeLoading = it is SwapState.FetchingFee

                    _openConfirmation.postValue(it == SwapState.SwapAllowed)

                    if (it == SwapState.Success) {
                        _closeWithSuccess.postValue(R.string.Hud_Text_Success)
                    }
                }
                .let { disposables.add(it) }
    }

    // region Inputs
    fun setCoinSending(coin: Coin) {
        swapService.enterCoinSending(coin)
    }

    fun setCoinReceiving(coin: Coin) {
        swapService.enterCoinReceiving(coin)
    }

    fun setAmountSending(amount: String?) {
        swapService.enterAmountSending(amount?.toBigDecimalOrNull())
    }

    fun setAmountReceiving(amount: String?) {
        swapService.enterAmountReceiving(amount?.toBigDecimalOrNull())
    }

    fun onProceedClick() {
        swapService.proceed()
    }

    fun onApproved() {
        swapService.approved()
    }

    fun onSwitchClick() {
        swapService.switchCoins()
    }

    fun onSwapSettingsUpdated(swapSettings: SwapSettings) {
        swapService.updateSwapSettings(swapSettings)
    }

    fun onSettingsClick() {
        _openSettings.postValue(Pair(swapService.currentSwapSettings, swapService.defaultSwapSettings))
    }

    // endregion

    override fun onCleared() {
        disposables.dispose()

        clearables.forEach {
            it.clear()
        }
    }

    @Synchronized
    private fun updateLoading() {
        _loading.postValue(feeLoading || approving || tradeItemLoading)
    }

    private fun tradeViewItem(trade: Trade): TradeViewItem {
        return TradeViewItem(
                null,//trade.executionPrice?.let { formatter.executionPrice(it, trade.coinSending, trade.coinReceiving) },
                null,//trade.priceImpact?.let { formatter.priceImpact(it) },
                trade.priceImpact?.level,
                "minMaxTitle",//formatter.minMaxTitle(trade.amountType),
                null//trade.minMaxAmount?.let { formatter.minMaxValue(it, trade.coinSending, trade.coinReceiving, trade.amountType) }
        )
    }

    private fun errorText(errors: List<SwapError>): String? {
        return when {
            errors.contains(SwapError.NoLiquidity) -> stringProvider.string(R.string.Swap_ErrorNoLiquidity)
            errors.contains(SwapError.CouldNotFetchTrade) -> stringProvider.string(R.string.Swap_ErrorCouldNotFetchTrade)
            errors.contains(SwapError.CouldNotFetchAllowance) -> stringProvider.string(R.string.Swap_ErrorCouldNotFetchAllowance)
            errors.contains(SwapError.CouldNotFetchFee) -> stringProvider.string(R.string.Swap_ErrorCouldNotFetchFee)
            errors.any { it is SwapError.InsufficientBalanceForFee } -> {
                val error = errors.first { it is SwapError.InsufficientBalanceForFee } as SwapError.InsufficientBalanceForFee
                val coinValue = error.coinValue

                stringProvider.string(R.string.Approve_InsufficientFeeAlert, coinValue.coin.title, App.numberFormatter.formatCoin(coinValue.value, coinValue.coin.code, 0, 8))
            }
            errors.any { it is SwapError.InsufficientFeeCoinBalance } -> {
                stringProvider.string(R.string.Swap_ErrorInsufficientEthBalance)
            }
            errors.any { it is SwapError.Other } -> {
                val error = errors.first { it is SwapError.Other } as SwapError.Other
                error.error.message ?: error.error.javaClass.simpleName
            }
            else -> null
        }
    }

}
