package io.horizontalsystems.bankwallet.modules.swap.oneinch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
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
    private val proceedActionLiveData = MutableLiveData<ActionState>(ActionState.Hidden)
    private val approveActionLiveData = MutableLiveData<ActionState>(ActionState.Hidden)
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
    fun proceedActionLiveData(): LiveData<ActionState> = proceedActionLiveData
    fun approveActionLiveData(): LiveData<ActionState> = approveActionLiveData
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
                syncApproveAction()
                syncProceedAction()
            }.let { disposables.add(it) }
    }

    private fun sync(serviceState: OneInchSwapService.State) {
        isLoadingLiveData.postValue(serviceState == OneInchSwapService.State.Loading)
        syncProceedAction()
    }

    private fun convert(error: Throwable): String =
        when (val convertedError = error.convertedError) {
            is JsonRpc.ResponseError.RpcError -> {
                convertedError.error.message
            }
            else -> {
                convertedError.message ?: convertedError.javaClass.simpleName
            }
        }

    private fun sync(errors: List<Throwable>) {
        val filtered =
            errors.filter { it !is EvmTransactionService.GasDataError && it !is SwapError }
        swapErrorLiveData.postValue(filtered.firstOrNull()?.let { convert(it) })

        syncProceedAction()
        syncApproveAction()
    }

    private fun syncProceedAction() {
        val proceedAction = when {
            service.state is OneInchSwapService.State.Ready -> {
                ActionState.Enabled(Translator.getString(R.string.Swap_Proceed))
            }
            tradeService.state is OneInchTradeService.State.Ready -> {
                when {
                    service.errors.any { it == SwapError.InsufficientBalanceFrom } -> {
                        ActionState.Disabled(Translator.getString(R.string.Swap_ErrorInsufficientBalance))
                    }
                    service.errors.any { it == SwapError.ForbiddenPriceImpactLevel } -> {
                        ActionState.Disabled(Translator.getString(R.string.Swap_ErrorHighPriceImpact))
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
        proceedActionLiveData.postValue(proceedAction)
    }

    private fun syncApproveAction() {
        val approveAction: ActionState
        val approveStep: ApproveStep
        when {
            pendingAllowanceService.state == SwapPendingAllowanceState.Pending -> {
                approveAction = ActionState.Disabled(Translator.getString(R.string.Swap_Approving))
                approveStep = ApproveStep.Approving
            }
            tradeService.state is OneInchTradeService.State.NotReady || service.errors.any { it == SwapError.InsufficientBalanceFrom } -> {
                approveAction = ActionState.Hidden
                approveStep = ApproveStep.NA
            }
            service.errors.any { it == SwapError.InsufficientAllowance } -> {
                approveAction = ActionState.Enabled(Translator.getString(R.string.Swap_Approve))
                approveStep = ApproveStep.ApproveRequired
            }
            pendingAllowanceService.state == SwapPendingAllowanceState.Approved -> {
                approveAction = ActionState.Disabled(Translator.getString(R.string.Swap_Approve))
                approveStep = ApproveStep.Approved
            }
            else -> {
                approveAction = ActionState.Hidden
                approveStep = ApproveStep.NA
            }
        }
        approveActionLiveData.postValue(approveAction)
        approveStepLiveData.postValue(approveStep)
    }

    //region models
    sealed class ActionState {
        object Hidden : ActionState()
        class Enabled(val title: String) : ActionState()
        class Disabled(val title: String) : ActionState()
    }
    //endregion
}
