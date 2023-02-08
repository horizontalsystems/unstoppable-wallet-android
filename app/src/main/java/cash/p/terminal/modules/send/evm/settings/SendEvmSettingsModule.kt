package cash.p.terminal.modules.send.evm.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.modules.evmfee.IEvmGasPriceService

object SendEvmSettingsModule {

    class Factory(private val gasPriceService: IEvmGasPriceService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SendEvmSettingsViewModel(
                gasPriceService
            ) as T
        }
    }
}
