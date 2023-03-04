package io.horizontalsystems.bankwallet.modules.swap.allowance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class SwapAllowanceViewModel(
        private val service: SwapMainModule.ISwapService,
        private val allowanceService: SwapAllowanceService,
        private val pendingAllowanceService: SwapPendingAllowanceService,
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
            service.errorsObservable.asFlow()
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

    private fun syncVisible(state: SwapAllowanceService.State? = null) {
        val allowanceState = state ?: allowanceService.state

        isVisible = when {
            allowanceState == null -> false
            pendingAllowanceService.state.loading() -> true
            allowanceState is SwapAllowanceService.State.NotReady -> true
            else -> isError || revokeRequired
        }
    }

    private fun handle(errors: List<Throwable>) {
        isError = errors.any { it is SwapMainModule.SwapError.InsufficientAllowance }
        revokeRequired = errors.any { it is SwapMainModule.SwapError.RevokeAllowanceRequired }

        syncVisible()
        emitState()
    }

    private fun handle(allowanceState: SwapAllowanceService.State?) {
        syncVisible(allowanceState)

        allowanceState?.let {
            allowance = allowance(allowanceState)
        }

        emitState()
    }

    private fun allowance(allowanceState: SwapAllowanceService.State): String {
        return when (allowanceState) {
            SwapAllowanceService.State.Loading -> Translator.getString(R.string.Alert_Loading)
            is SwapAllowanceService.State.Ready -> allowanceState.allowance.let { formatter.coinAmount(it.value, it.coin.code) }
            is SwapAllowanceService.State.NotReady -> Translator.getString(R.string.NotAvailable)
        }
    }

    data class UiState(
        val isVisible: Boolean,
        val allowance: String?,
        val isError: Boolean,
        val revokeRequired: Boolean
    )

}
