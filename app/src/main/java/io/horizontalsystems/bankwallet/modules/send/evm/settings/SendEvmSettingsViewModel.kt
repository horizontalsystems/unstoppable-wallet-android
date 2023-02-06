package io.horizontalsystems.bankwallet.modules.send.evm.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmGasPriceService
import kotlinx.coroutines.launch

class SendEvmSettingsViewModel(
    private val gasPriceService: IEvmGasPriceService
) : ViewModel() {

    var isRecommendedSettingsSelected by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            gasPriceService.recommendedGasPriceSelectedFlow.collect {
                isRecommendedSettingsSelected = it
            }
        }
    }

    fun onClickReset() {
        gasPriceService.setRecommended()
    }

}
