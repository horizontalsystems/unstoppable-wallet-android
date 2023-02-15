package io.horizontalsystems.bankwallet.modules.send.evm.settings

import android.util.Log
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

            Log.e("e", "settingViewModel.init()")
            // service.start()
        }
    }

    private fun sync(state: DataState<SendEvmSettingsService.Transaction>) {
        when (state) {
            is DataState.Error -> {
                Log.e("e", "settings state: error ${state.error.message ?: state.error::class.simpleName}")
            }
            DataState.Loading -> {
                Log.e("e", "settings state: loading")
            }
            is DataState.Success -> {
                Log.e("e", "settings state: success nonce=${state.data.nonce}, recommended: ${state.data.default}")

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

        Log.e("e", "cautions: warnings=${warnings.firstOrNull()?.javaClass?.simpleName}, errors: ${errors.firstOrNull()?.javaClass?.simpleName}")

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
