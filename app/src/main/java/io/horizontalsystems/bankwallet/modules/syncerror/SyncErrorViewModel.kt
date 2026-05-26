package io.horizontalsystems.bankwallet.modules.syncerror

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.entities.Wallet

@HiltViewModel(assistedFactory = SyncErrorViewModel.Factory::class)
class SyncErrorViewModel @AssistedInject constructor(
    @Assisted wallet: Wallet,
    adapterManager: IAdapterManager,
    appConfigProvider: AppConfigProvider,
    btcBlockchainManager: BtcBlockchainManager,
    evmBlockchainManager: EvmBlockchainManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(wallet: Wallet): SyncErrorViewModel
    }

    private val service = SyncErrorService(
        wallet,
        adapterManager,
        appConfigProvider.reportEmail,
        btcBlockchainManager,
        evmBlockchainManager,
    )

    val sourceChangeable by service::sourceChangeable
    val blockchainWrapper by service::blockchainWrapper
    val coinName by service::coinName
    val reportEmail by service::reportEmail

    fun retry() {
        service.retry()
    }

}
