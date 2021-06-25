package io.horizontalsystems.bankwallet.modules.swap.uniswap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapError
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.settings.uniswap.SwapTradeOptions
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.uniswapkit.TradeError
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class UniswapViewModel(
        val service: UniswapService,
        val tradeService: UniswapTradeService,
        private val pendingAllowanceService: SwapPendingAllowanceService,
        private val formatter: SwapViewItemHelper
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val isLoadingLiveData = MutableLiveData(false)
    private val swapErrorLiveData = MutableLiveData<String?>(null)
    private val tradeViewItemLiveData = MutableLiveData<TradeViewItem?>(null)
    private val tradeOptionsViewItemLiveData = MutableLiveData<TradeOptionsViewItem?>(null)
    private val proceedActionLiveData = MutableLiveData<ActionState>(ActionState.Hidden)
    private val approveActionLiveData = MutableLiveData<ActionState>(ActionState.Hidden)
    private val openApproveLiveEvent = SingleLiveEvent<SwapAllowanceService.ApproveData>()
    private val advancedSettingsVisibleLiveData = MutableLiveData(false)
    private val openConfirmationLiveEvent = SingleLiveEvent<SendEvmData>()

    init {
        subscribeToServices()

        sync(service.state)
        sync(service.errors)
        sync(tradeService.state)
    }

    //region outputs
    fun isLoadingLiveData(): LiveData<Boolean> = isLoadingLiveData
    fun swapErrorLiveData(): LiveData<String?> = swapErrorLiveData
    fun tradeViewItemLiveData(): LiveData<TradeViewItem?> = tradeViewItemLiveData
    fun tradeOptionsViewItemLiveData(): LiveData<TradeOptionsViewItem?> = tradeOptionsViewItemLiveData
    fun proceedActionLiveData(): LiveData<ActionState> = proceedActionLiveData
    fun approveActionLiveData(): LiveData<ActionState> = approveActionLiveData
    fun openApproveLiveEvent(): LiveData<SwapAllowanceService.ApproveData> = openApproveLiveEvent
    fun advancedSettingsVisibleLiveData(): LiveData<Boolean> = advancedSettingsVisibleLiveData
    fun openConfirmationLiveEvent(): LiveData<SendEvmData> = openConfirmationLiveEvent

    fun onTapSwitch() {
        tradeService.switchCoins()
    }

    fun onTapApprove() {
        service.approveData?.let { approveData ->
            openApproveLiveEvent.postValue(approveData)
        }
    }

    fun onTapProceed() {
        val serviceState = service.state
        if (serviceState is UniswapService.State.Ready) {
            val trade = (tradeService.state as? UniswapTradeService.State.Ready)?.trade
            val swapInfo = SendEvmData.SwapInfo(
                    estimatedIn = tradeService.amountFrom ?: BigDecimal.ZERO,
                    estimatedOut = tradeService.amountTo ?: BigDecimal.ZERO,
                    slippage = formatter.slippage(tradeService.tradeOptions.allowedSlippage),
                    deadline = formatter.deadline(tradeService.tradeOptions.ttl),
                    recipientDomain = tradeService.tradeOptions.recipient?.title,
                    price = formatter.price(trade?.tradeData?.executionPrice, tradeService.coinFrom, tradeService.coinTo),
                    priceImpact = trade?.let { formatter.priceImpactViewItem(it)?.value }
            )
            openConfirmationLiveEvent.postValue(SendEvmData(serviceState.transactionData, SendEvmData.AdditionalInfo.Swap(swapInfo)))
        }
    }

    fun didApprove() {
        pendingAllowanceService.syncAllowance()
    }

    fun restoreProviderState(swapProviderState: SwapMainModule.SwapProviderState) {
        tradeService.restoreState(swapProviderState)
    }

    fun getProviderState(): SwapMainModule.SwapProviderState {
        return SwapMainModule.SwapProviderState(tradeService.coinFrom, tradeService.coinTo, tradeService.amountFrom, tradeService.amountTo, tradeService.amountType)
    }
    //endregion

    override fun onCleared() {
        service.onCleared()
        disposables.clear()
    }

    private fun subscribeToServices() {
        service.stateObservable
                .subscribeOn(Schedulers.io())
                .subscribe { sync(it) }
                .let { disposables.add(it) }

        service.errorsObservable
                .subscribeOn(Schedulers.io())
                .subscribe { sync(it) }
                .let { disposables.add(it) }

        tradeService.stateObservable
                .subscribeOn(Schedulers.io())
                .subscribe { sync(it) }
                .let { disposables.add(it) }

        tradeService.tradeOptionsObservable
                .subscribeOn(Schedulers.io())
                .subscribe { sync(it) }
                .let { disposables.add(it) }

        pendingAllowanceService.isPendingObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncApproveAction()
                    syncProceedAction()
                }
                .let { disposables.add(it) }
    }

    private fun sync(serviceState: UniswapService.State) {
        isLoadingLiveData.postValue(serviceState == UniswapService.State.Loading)
        syncProceedAction()
    }

    private fun convert(error: Throwable): String = when (val convertedError = error.convertedError) {
        is JsonRpc.ResponseError.RpcError -> {
            convertedError.error.message
        }
        is TradeError.TradeNotFound -> {
            Translator.getString(R.string.Swap_ErrorNoLiquidity)
        }
        else -> {
            convertedError.message ?: convertedError.javaClass.simpleName
        }
    }

    private fun sync(errors: List<Throwable>) {
        val filtered = errors.filter { it !is EvmTransactionService.GasDataError && it !is SwapError }
        swapErrorLiveData.postValue(filtered.firstOrNull()?.let { convert(it) })

        syncProceedAction()
        syncApproveAction()
    }

    private fun sync(tradeServiceState: UniswapTradeService.State) {
        when (tradeServiceState) {
            is UniswapTradeService.State.Ready -> {
                tradeViewItemLiveData.postValue(tradeViewItem(tradeServiceState.trade))
                advancedSettingsVisibleLiveData.postValue(true)
            }
            else -> {
                tradeViewItemLiveData.postValue(null)
                advancedSettingsVisibleLiveData.postValue(false)
            }
        }
        syncProceedAction()
        syncApproveAction()
    }

    private fun sync(swapTradeOptions: SwapTradeOptions) {
        tradeOptionsViewItemLiveData.postValue(tradeOptionsViewItem(swapTradeOptions))
    }

    private fun syncProceedAction() {
        val proceedAction = when {
            service.state is UniswapService.State.Ready -> {
                ActionState.Enabled(Translator.getString(R.string.Swap_Proceed))
            }
            tradeService.state is UniswapTradeService.State.Ready -> {
                when {
                    service.errors.any { it == SwapError.InsufficientBalanceFrom } -> {
                        ActionState.Disabled(Translator.getString(R.string.Swap_ErrorInsufficientBalance))
                    }
                    service.errors.any { it == SwapError.ForbiddenPriceImpactLevel } -> {
                        ActionState.Disabled(Translator.getString(R.string.Swap_ErrorHighPriceImpact))
                    }
                    pendingAllowanceService.isPending -> {
                        ActionState.Hidden
                    }
                    else -> {
                        ActionState.Disabled(Translator.getString(R.string.Swap_Proceed))
                    }
                }
            }
            else -> {
                ActionState.Hidden
            }
        }
        proceedActionLiveData.postValue(proceedAction)
    }

    private fun syncApproveAction() {
        val approveAction = when {
            tradeService.state !is UniswapTradeService.State.Ready || service.errors.any { it == SwapError.InsufficientBalanceFrom || it == SwapError.ForbiddenPriceImpactLevel } -> {
                ActionState.Hidden
            }
            pendingAllowanceService.isPending -> {
                ActionState.Disabled(Translator.getString(R.string.Swap_Approving))
            }
            service.errors.any { it == SwapError.InsufficientAllowance } -> {
                ActionState.Enabled(Translator.getString(R.string.Swap_Approve))
            }
            else -> {
                ActionState.Hidden
            }
        }
        approveActionLiveData.postValue(approveAction)
    }

    private fun tradeViewItem(trade: UniswapTradeService.Trade): TradeViewItem {
        return TradeViewItem(
                formatter.price(trade.tradeData.executionPrice, tradeService.coinFrom, tradeService.coinTo),
                formatter.priceImpactViewItem(trade, UniswapTradeService.PriceImpactLevel.Warning),
                formatter.guaranteedAmountViewItem(trade.tradeData, tradeService.coinFrom, tradeService.coinTo)
        )
    }

    private fun tradeOptionsViewItem(tradeOptions: SwapTradeOptions): TradeOptionsViewItem {
        val defaultTradeOptions = TradeOptions()
        val slippage = if (tradeOptions.allowedSlippage.compareTo(defaultTradeOptions.allowedSlippagePercent) == 0) null else tradeOptions.allowedSlippage.stripTrailingZeros().toPlainString()
        val deadline = if (tradeOptions.ttl == defaultTradeOptions.ttl) null else tradeOptions.ttl.toString()
        val recipientAddress = tradeOptions.recipient?.hex

        return TradeOptionsViewItem(slippage, deadline, recipientAddress)
    }

    //region models
    data class TradeViewItem(
            val price: String? = null,
            val priceImpact: UniswapModule.PriceImpactViewItem? = null,
            val guaranteedAmount: UniswapModule.GuaranteedAmountViewItem? = null
    )

    data class TradeOptionsViewItem(
            val slippage: String?,
            val deadline: String?,
            val recipient: String?
    )

    sealed class ActionState {
        object Hidden : ActionState()
        class Enabled(val title: String) : ActionState()
        class Disabled(val title: String) : ActionState()
    }
    //endregion
}
