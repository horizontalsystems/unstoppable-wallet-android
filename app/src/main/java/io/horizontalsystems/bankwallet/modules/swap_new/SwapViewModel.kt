package io.horizontalsystems.bankwallet.modules.swap_new

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.core.ethereum.EthereumTransactionService
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.modules.swap_new.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap_new.allowance.SwapPendingAllowanceService
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.uniswapkit.TradeError
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SwapViewModel(
        val service: SwapService,
        val tradeService: SwapTradeService,
        private val pendingAllowanceService: SwapPendingAllowanceService,
        private val ethCoinService: CoinService,
        private val formatter: SwapViewItemHelper,
        private val stringProvider: StringProvider
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val isLoadingLiveData = MutableLiveData(false)
    private val swapErrorLiveData = MutableLiveData<String?>(null)
    private val tradeViewItemLiveData = MutableLiveData<TradeViewItem?>(null)
    private val tradeOptionsViewItemLiveData = MutableLiveData<TradeOptionsViewItem?>(null)
    private val proceedAllowedLiveData = MutableLiveData(false)
    private val approveActionLiveData = MutableLiveData(ApproveActionState.Hidden)
    private val openApproveLiveEvent = SingleLiveEvent<SwapAllowanceService.ApproveData>()

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
    fun proceedAllowedLiveData(): LiveData<Boolean> = proceedAllowedLiveData
    fun approveActionLiveData(): LiveData<ApproveActionState> = approveActionLiveData
    fun openApproveLiveEvent(): LiveData<SwapAllowanceService.ApproveData> = openApproveLiveEvent

    fun onTapSwitch() {
        tradeService.switchCoins()
    }
    fun onTapApprove() {
        service.approveData?.let { approveData ->
            openApproveLiveEvent.postValue(approveData)
        }
    }

    fun didApprove() {
        pendingAllowanceService.syncAllowance()
    }
    //endregion

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
                .subscribe { syncApproveAction() }
                .let { disposables.add(it) }
    }

    private fun sync(serviceState: SwapService.State) {
        isLoadingLiveData.postValue(serviceState == SwapService.State.Loading)
        proceedAllowedLiveData.postValue(serviceState == SwapService.State.Ready)
    }

    private fun convert(error: Throwable): String = when (error) {
        is SwapService.TransactionError.InsufficientBalance -> {
            stringProvider.string(R.string.EthereumTransaction_Error_InsufficientBalance, ethCoinService.coinValue(error.requiredBalance))
        }
        is JsonRpc.ResponseError.RpcError -> {
            val rpcErrorMessage = error.error.message
            if (rpcErrorMessage.contains("execution reverted") || rpcErrorMessage.contains("gas required exceeds"))
                stringProvider.string(R.string.Swap_ErrorInsufficientEthBalance)
            else
                rpcErrorMessage
        }
        is TradeError.TradeNotFound -> {
            stringProvider.string(R.string.Swap_ErrorNoLiquidity)
        }
        else -> {
            error.message ?: error.javaClass.simpleName
        }
    }

    private fun sync(errors: List<Throwable>) {
        val filtered = errors.filter { it !is EthereumTransactionService.GasDataError && it !is SwapService.SwapError }
        swapErrorLiveData.postValue(filtered.firstOrNull()?.let { convert(it) })

        syncApproveAction()
    }

    private fun sync(tradeServiceState: SwapTradeService.State) {
        when (tradeServiceState) {
            is SwapTradeService.State.Ready -> {
                tradeViewItemLiveData.postValue(tradeViewItem(tradeServiceState.trade))
            }
            else -> tradeViewItemLiveData.postValue(null)
        }
    }

    private fun sync(tradeOptions: TradeOptions) {
        tradeOptionsViewItemLiveData.postValue(tradeOptionsViewItem(tradeOptions))
    }

    private fun syncApproveAction() {
        if (pendingAllowanceService.isPending) {
            approveActionLiveData.postValue(ApproveActionState.Pending)
        } else {
            val isInsufficientAllowance = service.errors.any { it == SwapService.SwapError.InsufficientAllowance }
            approveActionLiveData.postValue(if (isInsufficientAllowance) ApproveActionState.Visible else ApproveActionState.Hidden)
        }
    }

    private fun tradeViewItem(trade: SwapTradeService.Trade): TradeViewItem {
        return TradeViewItem(
                formatter.price(trade.tradeData.executionPrice, tradeService.coinFrom, tradeService.coinTo),
                formatter.priceImpactViewItem(trade, SwapTradeService.PriceImpactLevel.Warning),
                formatter.guaranteedAmountViewItem(trade.tradeData, tradeService.coinFrom, tradeService.coinTo)
        )
    }

    private fun tradeOptionsViewItem(tradeOptions: TradeOptions): TradeOptionsViewItem {
        val defaultTradeOptions = TradeOptions()
        val slippage = if (tradeOptions.allowedSlippagePercent.compareTo(defaultTradeOptions.allowedSlippagePercent) == 0) null else tradeOptions.allowedSlippagePercent.stripTrailingZeros().toPlainString()
        val deadline = if (tradeOptions.ttl == defaultTradeOptions.ttl) null else tradeOptions.ttl.toString()
        val recipientAddress = tradeOptions.recipient?.hex

        return TradeOptionsViewItem(slippage, deadline, recipientAddress)
    }

    //region models
    data class TradeViewItem(
            val price: String? = null,
            val priceImpact: SwapModule.PriceImpactViewItem? = null,
            val guaranteedAmount: SwapModule.GuaranteedAmountViewItem? = null
    )

    data class TradeOptionsViewItem(
            val slippage: String?,
            val deadline: String?,
            val recipient: String?
    )

    enum class ApproveActionState {
        Hidden, Visible, Pending
    }
    //endregion
}
