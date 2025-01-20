package io.horizontalsystems.bankwallet.modules.send.evm.processing

import io.horizontalsystems.bankwallet.core.ViewModelUiState

class SendEvmProcessingViewModel: ViewModelUiState<SendEvmProcessingUiState>() {

    override fun createState(): SendEvmProcessingUiState {
            TODO("Not yet implemented")
    }
}

data class SendEvmProcessingUiState(
    val processing: Boolean
)