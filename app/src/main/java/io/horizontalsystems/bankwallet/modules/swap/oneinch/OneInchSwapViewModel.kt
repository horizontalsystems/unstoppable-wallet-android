package io.horizontalsystems.bankwallet.modules.swap.oneinch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.evmfee.GasDataError
import io.horizontalsystems.bankwallet.modules.swap.SwapActionState
import io.horizontalsystems.bankwallet.modules.swap.SwapButtons
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapError
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceState
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.RoundingMode

class OneInchSwapViewModel(
    val service: OneInchSwapService,
    val tradeService: OneInchTradeService,
    private val pendingAllowanceService: SwapPendingAllowanceService,
    private val formatter: SwapViewItemHelper
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val isLoadingLiveData = MutableLiveData(false)
    private val swapErrorLiveData = MutableLiveData<String?>(null)
    private val buttonsLiveData = MutableLiveData<SwapButtons>()
    private val tradeViewItemLiveData = MutableLiveData<TradeViewItem?>(null)
    private val tradeTimeoutProgressLiveData = MutableLiveData<Float>()

    init {
        subscribeToServices()

        sync(service.state)
        sync(tradeService.state)
        sync(service.errors)
    }

    //region outputs
    fun isLoadingLiveData(): LiveData<Boolean> = isLoadingLiveData
    fun swapErrorLiveData(): LiveData<String?> = swapErrorLiveData
    fun buttonsLiveData(): LiveData<SwapButtons> = buttonsLiveData
    fun tradeViewItemLiveData(): LiveData<TradeViewItem?> = tradeViewItemLiveData
    fun tradeTimeoutProgressLiveData(): LiveData<Float> = tradeTimeoutProgressLiveData

    val revokeEvmData by service::revokeEvmData
    val blockchainType by service::blockchainType
    val approveData by service::approveData

    val proceedParams: OneInchSwapParameters?
        get() {
            val serviceState = service.state
            val tradeServiceState = tradeService.state
            return if (serviceState is OneInchSwapService.State.Ready && tradeServiceState is OneInchTradeService.State.Ready) {
                tradeServiceState.params
            } else
                null
        }

    fun onTapSwitch() {
        tradeService.switchCoins()
    }

    fun didApprove() {
        pendingAllowanceService.syncAllowance()
    }

    fun getProviderState() = SwapMainModule.SwapProviderState(
        tradeService.tokenFrom,
        tradeService.tokenTo,
        tradeService.amountFrom,
        tradeService.amountTo,
        tradeService.amountType
    )

    fun restoreProviderState(swapProviderState: SwapMainModule.SwapProviderState) {
        tradeService.restoreState(swapProviderState)
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
            }.let { disposables.add(it) }

        tradeService.timeoutProgressObservable
            .subscribeOn(Schedulers.io())
            .subscribe {
                tradeTimeoutProgressLiveData.postValue(it)
            }
            .let { disposables.add(it) }
    }

    private fun sync(tradeServiceState: OneInchTradeService.State) {

        when (tradeServiceState) {
            is OneInchTradeService.State.Ready -> {
                tradeViewItemLiveData.postValue(tradeViewItem(tradeServiceState.params))
            }
            OneInchTradeService.State.Loading -> {
                tradeViewItemLiveData.postValue(tradeViewItemLiveData.value?.copy(expired = true))
            }
            is OneInchTradeService.State.NotReady -> {
                tradeViewItemLiveData.postValue(null)
            }
        }
    }

    private fun tradeViewItem(params: OneInchSwapParameters) = try {
        val sellPrice = params.amountTo.divide(params.amountFrom, params.tokenFrom.decimals, RoundingMode.HALF_UP).stripTrailingZeros()
        val buyPrice = params.amountFrom.divide(params.amountTo, params.tokenTo.decimals, RoundingMode.HALF_UP).stripTrailingZeros()
        val (primaryPrice, secondaryPrice) = formatter.prices(sellPrice, buyPrice, tradeService.tokenFrom, tradeService.tokenTo)
        TradeViewItem(primaryPrice, secondaryPrice)
    } catch (exception: ArithmeticException) {
        null
    }

    private fun sync(serviceState: OneInchSwapService.State) {
        isLoadingLiveData.postValue(serviceState == OneInchSwapService.State.Loading)
        syncState()
    }

    private fun convert(error: Throwable): String =
        when (val convertedError = error.convertedError) {
            is JsonRpc.ResponseError.RpcError -> {
                convertedError.error.message
            }
            is EvmError.InsufficientLiquidity -> {
                Translator.getString(R.string.EthereumTransaction_Error_InsufficientLiquidity)
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
        service.state is OneInchSwapService.State.Ready -> {
            SwapActionState.Enabled(Translator.getString(R.string.Swap_Proceed))
        }
        tradeService.state is OneInchTradeService.State.Ready -> {
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
        tradeService.state is OneInchTradeService.State.NotReady || service.errors.any { it == SwapError.InsufficientBalanceFrom } -> {
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

    data class TradeViewItem(
        val primaryPrice: String? = null,
        val secondaryPrice: String? = null,
        val expired: Boolean = false
    )

}
