package io.horizontalsystems.bankwallet.modules.swap.uniswap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.evmfee.GasDataError
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.SwapActionState
import io.horizontalsystems.bankwallet.modules.swap.SwapButtons
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ApproveStep
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapError
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceState
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
    private val buttonsLiveData = MutableLiveData<SwapButtons>()
    private val approveStepLiveData = MutableLiveData(ApproveStep.NA)

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
    fun buttonsLiveData(): LiveData<SwapButtons> = buttonsLiveData
    fun approveStepLiveData(): LiveData<ApproveStep> = approveStepLiveData

    val revokeEvmData by service::revokeEvmData
    val blockchainType by service::blockchainType
    val approveData by service::approveData

    val proceedParams: SendEvmData?
        get() {
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
                        tradeService.tokenFrom,
                        tradeService.tokenTo
                    ),
                    priceImpact = trade?.let { formatter.priceImpactViewItem(it) }
                )
                val warnings: List<Warning> = if (trade?.priceImpactLevel == UniswapTradeService.PriceImpactLevel.Forbidden)
                    listOf(UniswapModule.UniswapWarnings.PriceImpactWarning)
                else
                    listOf()

                return SendEvmData(
                        serviceState.transactionData,
                        SendEvmData.AdditionalInfo.Uniswap(swapInfo),
                        warnings
                    )
            }
            return null
        }

    fun onTapSwitch() {
        tradeService.switchCoins()
    }

    fun didApprove() {
        pendingAllowanceService.syncAllowance()
    }

    fun restoreProviderState(swapProviderState: SwapMainModule.SwapProviderState) {
        tradeService.restoreState(swapProviderState)
    }

    fun getProviderState(): SwapMainModule.SwapProviderState {
        return SwapMainModule.SwapProviderState(
            tradeService.tokenFrom,
            tradeService.tokenTo,
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
            errors.filter { it !is GasDataError && it !is SwapError }
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
        val revokeAction = getRevokeActionState()
        val approveAction = getApproveActionState(revokeAction)
        val proceedAction = getProceedActionState(revokeAction)
        val approveStep = getApproveStep(revokeAction)
        buttonsLiveData.postValue(SwapButtons(revokeAction, approveAction, proceedAction))
        approveStepLiveData.postValue(approveStep)
    }

    private fun getProceedActionState(revokeAction: SwapActionState): SwapActionState {
        return when {
            revokeAction !is SwapActionState.Hidden -> {
                SwapActionState.Hidden
            }
            service.state is UniswapService.State.Ready -> {
                SwapActionState.Enabled(Translator.getString(R.string.Swap_Proceed))
            }
            tradeService.state is UniswapTradeService.State.Ready -> {
                when {
                    service.errors.any { it == SwapError.InsufficientBalanceFrom } -> {
                        SwapActionState.Disabled(Translator.getString(R.string.Swap_ErrorInsufficientBalance))
                    }
                    pendingAllowanceService.state == SwapPendingAllowanceState.Approving -> {
                        SwapActionState.Disabled(Translator.getString(R.string.Swap_Proceed))
                    }
                    else -> {
                        SwapActionState.Disabled(Translator.getString(R.string.Swap_Proceed))
                    }
                }
            }
            else -> {
                SwapActionState.Disabled(Translator.getString(R.string.Swap_Proceed))
            }
        }
    }

    private fun getRevokeActionState() = when {
        pendingAllowanceService.state == SwapPendingAllowanceState.Revoking -> {
            SwapActionState.Disabled(Translator.getString(R.string.Swap_Revoking))
        }
        service.errors.any { it == SwapError.RevokeAllowanceRequired } -> {
            SwapActionState.Enabled(Translator.getString(R.string.Swap_Revoke))
        }
        else -> {
            SwapActionState.Hidden
        }
    }

    private fun getApproveActionState(revokeAction: SwapActionState): SwapActionState {
        return when {
            revokeAction !is SwapActionState.Hidden -> {
                SwapActionState.Hidden
            }
            pendingAllowanceService.state == SwapPendingAllowanceState.Approving -> {
                SwapActionState.Disabled(Translator.getString(R.string.Swap_Approving))
            }
            tradeService.state is UniswapTradeService.State.NotReady || service.errors.any { it == SwapError.InsufficientBalanceFrom } -> {
                SwapActionState.Hidden
            }
            service.errors.any { it == SwapError.InsufficientAllowance } -> {
                SwapActionState.Enabled(Translator.getString(R.string.Swap_Approve))
            }
            pendingAllowanceService.state == SwapPendingAllowanceState.Approved -> {
                SwapActionState.Disabled(Translator.getString(R.string.Swap_Approve))
            }
            else -> {
                SwapActionState.Hidden
            }
        }
    }

    private fun getApproveStep(revokeAction: SwapActionState): ApproveStep {
        return when {
            revokeAction !is SwapActionState.Hidden -> {
                ApproveStep.NA
            }
            pendingAllowanceService.state == SwapPendingAllowanceState.Approving -> {
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
                quoteToken = tradeService.tokenFrom,
                baseToken = tradeService.tokenTo
            ),
            sellPrice = formatter.price(
                Price(
                    baseTokenAmount = trade.tradeData.trade.tokenAmountOut,
                    quoteTokenAmount = trade.tradeData.trade.tokenAmountIn
                ).decimalValue,
                quoteToken = tradeService.tokenTo,
                baseToken = tradeService.tokenFrom
            ),
            priceImpact = formatter.priceImpactViewItem(trade, UniswapTradeService.PriceImpactLevel.Warning),
            guaranteedAmount = formatter.guaranteedAmountViewItem(
                trade.tradeData,
                tradeService.tokenFrom,
                tradeService.tokenTo
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
    
    //endregion
}
