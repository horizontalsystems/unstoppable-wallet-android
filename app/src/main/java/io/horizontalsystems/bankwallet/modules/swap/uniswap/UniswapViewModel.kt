package io.horizontalsystems.bankwallet.modules.swap.uniswap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionFeeService
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ApproveStep
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapError
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceState
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.uniswapkit.TradeError
import io.horizontalsystems.uniswapkit.models.Price
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
    private val buttonsLiveData = MutableLiveData<Buttons>()
    private val approveStepLiveData = MutableLiveData(ApproveStep.NA)
    private val openApproveLiveEvent = SingleLiveEvent<SwapAllowanceService.ApproveData>()
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
    fun buttonsLiveData(): LiveData<Buttons> = buttonsLiveData
    fun approveStepLiveData(): LiveData<ApproveStep> = approveStepLiveData
    fun openApproveLiveEvent(): LiveData<SwapAllowanceService.ApproveData> = openApproveLiveEvent
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
            val swapInfo = SendEvmData.UniswapInfo(
                estimatedIn = tradeService.amountFrom ?: BigDecimal.ZERO,
                estimatedOut = tradeService.amountTo ?: BigDecimal.ZERO,
                slippage = formatter.slippage(tradeService.tradeOptions.allowedSlippage),
                deadline = formatter.deadline(tradeService.tradeOptions.ttl),
                recipientDomain = tradeService.tradeOptions.recipient?.title,
                price = formatter.price(
                    trade?.tradeData?.executionPrice,
                    tradeService.coinFrom,
                    tradeService.coinTo
                ),
                priceImpact = trade?.let { formatter.priceImpactViewItem(it)},
                priceImpactWarning = trade?.priceImpactLevel == UniswapTradeService.PriceImpactLevel.Forbidden
            )
            openConfirmationLiveEvent.postValue(
                SendEvmData(
                    serviceState.transactionData,
                    SendEvmData.AdditionalInfo.Uniswap(swapInfo)
                )
            )
        }
    }

    fun didApprove() {
        pendingAllowanceService.syncAllowance()
    }

    fun restoreProviderState(swapProviderState: SwapMainModule.SwapProviderState) {
        tradeService.restoreState(swapProviderState)
    }

    fun getProviderState(): SwapMainModule.SwapProviderState {
        return SwapMainModule.SwapProviderState(
            tradeService.coinFrom,
            tradeService.coinTo,
            tradeService.amountFrom,
            tradeService.amountTo,
            tradeService.amountType
        )
    }

    fun onStart() {
        service.start()
    }

    fun onStop() {
        service.stop()
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

        pendingAllowanceService.stateObservable
            .subscribeOn(Schedulers.io())
            .subscribe {
                syncState()
            }
            .let { disposables.add(it) }
    }

    private fun sync(serviceState: UniswapService.State) {
        isLoadingLiveData.postValue(serviceState == UniswapService.State.Loading)
        syncState()
    }

    private fun convert(error: Throwable): String =
        when (val convertedError = error.convertedError) {
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
        val filtered =
            errors.filter { it !is EvmTransactionFeeService.GasDataError && it !is SwapError }
        swapErrorLiveData.postValue(filtered.firstOrNull()?.let { convert(it) })

        syncState()
    }

    private fun sync(tradeServiceState: UniswapTradeService.State) {
        when (tradeServiceState) {
            is UniswapTradeService.State.Ready -> {
                tradeViewItemLiveData.postValue(tradeViewItem(tradeServiceState.trade))
            }
            else -> {
                tradeViewItemLiveData.postValue(null)
            }
        }
        syncState()
    }

    private fun syncState() {
        val approveAction = getApproveActionState()
        val proceedAction = getProceedActionState()
        val approveStep = getApproveStep()
        buttonsLiveData.postValue(Buttons(approveAction, proceedAction))
        approveStepLiveData.postValue(approveStep)
    }

    private fun getProceedActionState(): ActionState {
        return when {
            service.state is UniswapService.State.Ready -> {
                ActionState.Enabled(Translator.getString(R.string.Swap_Proceed))
            }
            tradeService.state is UniswapTradeService.State.Ready -> {
                when {
                    service.errors.any { it == SwapError.InsufficientBalanceFrom } -> {
                        ActionState.Disabled(Translator.getString(R.string.Swap_ErrorInsufficientBalance))
                    }
                    pendingAllowanceService.state == SwapPendingAllowanceState.Pending -> {
                        ActionState.Disabled(Translator.getString(R.string.Swap_Proceed))
                    }
                    else -> {
                        ActionState.Disabled(Translator.getString(R.string.Swap_Proceed))
                    }
                }
            }
            else -> {
                ActionState.Disabled(Translator.getString(R.string.Swap_Proceed))
            }
        }
    }

    private fun getApproveActionState(): ActionState {
        return when {
            pendingAllowanceService.state == SwapPendingAllowanceState.Pending -> {
                ActionState.Disabled(Translator.getString(R.string.Swap_Approving))
            }
            tradeService.state is UniswapTradeService.State.NotReady || service.errors.any { it == SwapError.InsufficientBalanceFrom } -> {
                ActionState.Hidden
            }
            service.errors.any { it == SwapError.InsufficientAllowance } -> {
                ActionState.Enabled(Translator.getString(R.string.Swap_Approve))
            }
            pendingAllowanceService.state == SwapPendingAllowanceState.Approved -> {
                ActionState.Disabled(Translator.getString(R.string.Swap_Approve))
            }
            else -> {
                ActionState.Hidden
            }
        }
    }

    private fun getApproveStep(): ApproveStep {
        return when {
            pendingAllowanceService.state == SwapPendingAllowanceState.Pending -> {
                ApproveStep.Approving
            }
            tradeService.state is UniswapTradeService.State.NotReady || service.errors.any { it == SwapError.InsufficientBalanceFrom } -> {
                ApproveStep.NA
            }
            service.errors.any { it == SwapError.InsufficientAllowance } -> {
                ApproveStep.ApproveRequired
            }
            pendingAllowanceService.state == SwapPendingAllowanceState.Approved -> {
                ApproveStep.Approved
            }
            else -> {
                ApproveStep.NA
            }
        }
    }

    private fun tradeViewItem(trade: UniswapTradeService.Trade): TradeViewItem {
        return TradeViewItem(
            buyPrice = formatter.price(
                trade.tradeData.executionPrice,
                quoteCoin = tradeService.coinFrom,
                baseCoin = tradeService.coinTo
            ),
            sellPrice = formatter.price(
                Price(
                    baseTokenAmount = trade.tradeData.trade.tokenAmountOut,
                    quoteTokenAmount = trade.tradeData.trade.tokenAmountIn
                ).decimalValue,
                quoteCoin = tradeService.coinTo,
                baseCoin = tradeService.coinFrom
            ),
            priceImpact = formatter.priceImpactViewItem(trade, UniswapTradeService.PriceImpactLevel.Warning),
            guaranteedAmount = formatter.guaranteedAmountViewItem(
                trade.tradeData,
                tradeService.coinFrom,
                tradeService.coinTo
            )
        )
    }

    //region models
    data class TradeViewItem(
        val buyPrice: String? = null,
        val sellPrice: String? = null,
        val priceImpact: UniswapModule.PriceImpactViewItem? = null,
        val guaranteedAmount: UniswapModule.GuaranteedAmountViewItem? = null
    )

    sealed class ActionState {
        object Hidden : ActionState()
        class Enabled(val title: String) : ActionState()
        class Disabled(val title: String) : ActionState()
    }

    data class Buttons(val approve: ActionState, val proceed: ActionState)
    //endregion
}
