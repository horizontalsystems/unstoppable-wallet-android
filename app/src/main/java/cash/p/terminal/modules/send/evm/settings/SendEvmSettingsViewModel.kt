package cash.p.terminal.modules.send.evm.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.Warning
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.ethereum.CautionViewItemFactory
import cash.p.terminal.entities.DataState
import kotlinx.coroutines.launch

class SendEvmSettingsViewModel(
    private val service: SendEvmSettingsService,
    private val cautionViewItemFactory: CautionViewItemFactory
) : ViewModel() {

    var nonce by mutableStateOf<Long?>(null)
        private set

    var cautions by mutableStateOf<List<CautionViewItem>>(listOf())
        private set

    var isRecommendedSettingsSelected by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            service.stateFlow.collect {
                sync(it)
            }
        }
    }

    private fun sync(state: DataState<SendEvmSettingsService.Transaction>) {
        when (state) {
            is DataState.Error -> {
                isRecommendedSettingsSelected = false
            }
            DataState.Loading -> {
            }
            is DataState.Success -> {
                nonce = state.data.nonce
                isRecommendedSettingsSelected = state.data.default
            }
        }
        syncCautions(state)
    }

    private fun syncCautions(state: DataState<SendEvmSettingsService.Transaction>) {
        val warnings = mutableListOf<Warning>()
        val errors = mutableListOf<Throwable>()

        if (state is DataState.Error) {
            errors.add(state.error)
        } else if (state is DataState.Success) {
            warnings.addAll(state.data.warnings)
            errors.addAll(state.data.errors)
        }

        cautions = cautionViewItemFactory.cautionViewItems(warnings, errors)
    }

    fun onClickReset() {
        viewModelScope.launch {
            service.reset()
        }
    }

    fun onEnterNonce(nonce: Long) {
        service.setNonce(nonce)
    }

    fun onIncrementNonce() {
        service.incrementNonce()
    }

    fun onDecrementNonce() {
        service.decrementNonce()
    }

}
