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
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapError
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceState
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.uniswapkit.TradeError
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.math.RoundingMode

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
    private val tradeTimeoutProgressLiveData = MutableLiveData<Float>()

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
    fun tradeTimeoutProgressLiveData(): LiveData<Float> = tradeTimeoutProgressLiveData

    val revokeEvmData by service::revokeEvmData
    val blockchainType by service::blockchainType
    val approveData by service::approveData

    val proceedParams: SendEvmData?
        get() {
            val serviceState = service.state
            if (serviceState is UniswapService.State.Ready) {
                val trade = (tradeService.state as? UniswapTradeService.State.Ready)?.trade
                val (primaryPrice, secondaryPrice) = trade?.tradeData?.executionPrice?.let {
                    val sellPrice = it
                    val buyPrice = BigDecimal.ONE.divide(sellPrice, sellPrice.scale(), RoundingMode.HALF_EVEN)
                    formatter.prices(sellPrice, buyPrice, tradeService.tokenFrom, tradeService.tokenTo)
                } ?: Pair(null, null)

                val swapInfo = SendEvmData.UniswapInfo(
                    estimatedIn = tradeService.amountFrom ?: BigDecimal.ZERO,
                    estimatedOut = tradeService.amountTo ?: BigDecimal.ZERO,
                    slippage = formatter.slippage(tradeService.tradeOptions.allowedSlippage),
                    deadline = formatter.deadline(tradeService.tradeOptions.ttl),
                    recipientDomain = tradeService.tradeOptions.recipient?.title,
                    price = primaryPrice,
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

        service.balanceFromObservable
            .subscribeOn(Schedulers.io())
            .subscribe { syncState() }
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

        tradeService.timeoutProgressObservable
            .subscribeOn(Schedulers.io())
            .subscribe {
                tradeTimeoutProgressLiveData.postValue(it)
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
            UniswapTradeService.TradeServiceError.WrapUnwrapNotAllowed -> {
                Translator.getString(R.string.Swap_ErrorWrapUnwrapNotAllowed)
            }
            else -> {
                convertedError.message ?: convertedError.javaClass.simpleName
            }
        }

    private fun sync(errors: List<Throwable>) {
        val filtered = errors.filter { it !is GasDataError && it !is SwapError }
        swapErrorLiveData.postValue(filtered.firstOrNull()?.let { convert(it) })

        syncState()
    }

    private fun sync(tradeServiceState: UniswapTradeService.State) {
        when (tradeServiceState) {
            is UniswapTradeService.State.Ready -> {
                tradeViewItemLiveData.postValue(tradeViewItem(tradeServiceState.trade))
            }
            UniswapTradeService.State.Loading -> {
                tradeViewItemLiveData.postValue(tradeViewItemLiveData.value?.copy(expired = true))
            }
            is UniswapTradeService.State.NotReady -> {
                tradeViewItemLiveData.postValue(null)
            }
        }
        syncState()
    }

    private fun syncState() {
        val revokeAction = getRevokeActionState()
        val approveAction = getApproveActionState(revokeAction)
        val proceedAction = getProceedActionState(revokeAction)
        buttonsLiveData.postValue(SwapButtons(revokeAction, approveAction, proceedAction))
    }

    private fun getProceedActionState(revokeAction: SwapActionState) = when {
        service.balanceFrom == null -> {
            SwapActionState.Disabled(Translator.getString(R.string.Swap_ErrorBalanceNotAvailable))
        }
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

    private fun getRevokeActionState() = when {
        pendingAllowanceService.state == SwapPendingAllowanceState.Revoking -> {
            SwapActionState.Disabled(Translator.getString(R.string.Swap_Revoking))
        }
        service.errors.isNotEmpty() && service.errors.all { it == SwapError.RevokeAllowanceRequired } -> {
            SwapActionState.Enabled(Translator.getString(R.string.Swap_Revoke))
        }
        else -> {
            SwapActionState.Hidden
        }
    }

    private fun getApproveActionState(revokeAction: SwapActionState) = when {
        revokeAction !is SwapActionState.Hidden -> {
            SwapActionState.Hidden
        }
        pendingAllowanceService.state == SwapPendingAllowanceState.Approving -> {
            SwapActionState.Disabled(Translator.getString(R.string.Swap_Approving), loading = true)
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

    private fun tradeViewItem(trade: UniswapTradeService.Trade): TradeViewItem {

        val (primaryPrice, secondaryPrice) = trade.tradeData.executionPrice?.let {
            val sellPrice = it
            val buyPrice = BigDecimal.ONE.divide(sellPrice, sellPrice.scale(), RoundingMode.HALF_EVEN)
            formatter.prices(sellPrice, buyPrice, tradeService.tokenFrom, tradeService.tokenTo)
        } ?: Pair(null, null)

        return TradeViewItem(
            primaryPrice = primaryPrice,
            secondaryPrice = secondaryPrice,
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
        val primaryPrice: String? = null,
        val secondaryPrice: String? = null,
        val priceImpact: UniswapModule.PriceImpactViewItem? = null,
        val guaranteedAmount: UniswapModule.GuaranteedAmountViewItem? = null,
        val expired: Boolean = false
    )

    //endregion
}
