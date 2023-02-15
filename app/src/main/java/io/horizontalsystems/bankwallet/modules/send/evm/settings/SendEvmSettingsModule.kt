package io.horizontalsystems.bankwallet.modules.send.evm.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItemFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinService

object SendEvmSettingsModule {

    class Factory(
        private val settingsService: SendEvmSettingsService,
        private val evmCoinService: EvmCoinService
    ) : ViewModelProvider.Factory {

        private val cautionViewItemFactory by lazy { CautionViewItemFactory(evmCoinService) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SendEvmSettingsViewModel(
                settingsService,
                cautionViewItemFactory
            ) as T
        }
    }
}
