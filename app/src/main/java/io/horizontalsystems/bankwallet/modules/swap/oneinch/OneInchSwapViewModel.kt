package io.horizontalsystems.bankwallet.modules.swap.oneinch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionFeeService
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ApproveStep
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapError
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceState
import io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch.OneInchSwapParameters
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class OneInchSwapViewModel(
    val service: OneInchSwapService,
    val tradeService: OneInchTradeService,
    private val pendingAllowanceService: SwapPendingAllowanceService
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val isLoadingLiveData = MutableLiveData(false)
    private val swapErrorLiveData = MutableLiveData<String?>(null)
    private val buttonsLiveData = MutableLiveData<Buttons>()
    private val approveStepLiveData = MutableLiveData(ApproveStep.NA)
    private val openApproveLiveEvent = SingleLiveEvent<SwapAllowanceService.ApproveData>()
    private val openConfirmationLiveEvent = SingleLiveEvent<OneInchSwapParameters>()

    init {
        subscribeToServices()

        sync(service.state)
        sync(service.errors)
    }

    //region outputs
    fun isLoadingLiveData(): LiveData<Boolean> = isLoadingLiveData
    fun swapErrorLiveData(): LiveData<String?> = swapErrorLiveData
    fun buttonsLiveData(): LiveData<Buttons> = buttonsLiveData
    fun approveStepLiveData(): LiveData<ApproveStep> = approveStepLiveData
    fun openApproveLiveEvent(): LiveData<SwapAllowanceService.ApproveData> = openApproveLiveEvent
    fun openConfirmationLiveEvent(): LiveData<OneInchSwapParameters> = openConfirmationLiveEvent

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
        val tradeServiceState = tradeService.state
        if (serviceState is OneInchSwapService.State.Ready && tradeServiceState is OneInchTradeService.State.Ready) {
            openConfirmationLiveEvent.postValue(tradeServiceState.params)
        }
    }

    fun didApprove() {
        pendingAllowanceService.syncAllowance()
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

        pendingAllowanceService.stateObservable
            .subscribeOn(Schedulers.io())
            .subscribe {
                syncState()
            }.let { disposables.add(it) }
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
        val filtered =
            errors.filter { it !is EvmTransactionFeeService.GasDataError && it !is SwapError }
        swapErrorLiveData.postValue(filtered.firstOrNull()?.let { convert(it) })

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
            service.state is OneInchSwapService.State.Ready -> {
                ActionState.Enabled(Translator.getString(R.string.Swap_Proceed))
            }
            tradeService.state is OneInchTradeService.State.Ready -> {
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
            tradeService.state is OneInchTradeService.State.NotReady || service.errors.any { it == SwapError.InsufficientBalanceFrom } -> {
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
            tradeService.state is OneInchTradeService.State.NotReady || service.errors.any { it == SwapError.InsufficientBalanceFrom } -> {
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

    //region models
    sealed class ActionState {
        object Hidden : ActionState()
        class Enabled(val title: String) : ActionState()
        class Disabled(val title: String) : ActionState()
    }

    data class Buttons(val approve: ActionState, val proceed: ActionState)
    //endregion
}
