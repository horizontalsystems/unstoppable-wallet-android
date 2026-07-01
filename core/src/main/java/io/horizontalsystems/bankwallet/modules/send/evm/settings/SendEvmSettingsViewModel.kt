package io.horizontalsystems.bankwallet.modules.send.evm.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItemFactory
import io.horizontalsystems.bankwallet.entities.DataState
import kotlinx.coroutines.launch

class SendEvmSettingsViewModel(
    private val service: SendEvmSettingsService,
    private val cautionViewItemFactory: CautionViewItemFactory
) : ViewModel() {

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
}
