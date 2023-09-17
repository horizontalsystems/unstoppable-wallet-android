package cash.p.terminal.modules.swapx.allowance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.swap.SwapViewItemHelper
import cash.p.terminal.modules.swapx.ErrorShareService
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class SwapAllowanceViewModelX(
    private val errorShareService: ErrorShareService,
    private val allowanceService: SwapAllowanceServiceX,
    private val pendingAllowanceService: SwapPendingAllowanceServiceX,
    private val formatter: SwapViewItemHelper
) : ViewModel() {

    private var isVisible = false
    private var allowance: String? = null
    private var isError = false
    private var revokeRequired = false

    var uiState by mutableStateOf(
        UiState(
            isVisible = isVisible,
            allowance = allowance,
            isError = isError,
            revokeRequired = revokeRequired,
        )
    )
        private set

    init {
        viewModelScope.launch {
            allowanceService.stateObservable.asFlow()
                .collect { allowanceState ->
                    handle(allowanceState.orElse(null))
                }
        }
        viewModelScope.launch {
            errorShareService.errorsStateFlow
                .collect { errors ->
                    handle(errors)
                }
        }

        handle(allowanceService.state)
    }

    private fun emitState() {
        uiState = UiState(
            isVisible = isVisible,
            allowance = allowance,
            isError = isError,
            revokeRequired = revokeRequired,
        )
    }

    private fun syncVisible(state: SwapAllowanceServiceX.State? = null) {
        val allowanceState = state ?: allowanceService.state

        isVisible = when {
            allowanceState == null -> false
            pendingAllowanceService.state.loading() -> true
            allowanceState is SwapAllowanceServiceX.State.NotReady -> true
            else -> isError || revokeRequired
        }
    }

    private fun handle(errors: List<Throwable>) {
        isError = errors.any { it is SwapMainModule.SwapError.InsufficientAllowance }
        revokeRequired = errors.any { it is SwapMainModule.SwapError.RevokeAllowanceRequired }

        syncVisible()
        emitState()
    }

    private fun handle(allowanceState: SwapAllowanceServiceX.State?) {
        syncVisible(allowanceState)

        allowanceState?.let {
            allowance = allowance(allowanceState)
        }

        emitState()
    }

    private fun allowance(allowanceState: SwapAllowanceServiceX.State): String {
        return when (allowanceState) {
            SwapAllowanceServiceX.State.Loading -> Translator.getString(R.string.Alert_Loading)
            is SwapAllowanceServiceX.State.Ready -> allowanceState.allowance.let { formatter.coinAmount(it.value, it.coin.code) }
            is SwapAllowanceServiceX.State.NotReady -> Translator.getString(R.string.NotAvailable)
        }
    }

    data class UiState(
        val isVisible: Boolean,
        val allowance: String?,
        val isError: Boolean,
        val revokeRequired: Boolean
    )

}
