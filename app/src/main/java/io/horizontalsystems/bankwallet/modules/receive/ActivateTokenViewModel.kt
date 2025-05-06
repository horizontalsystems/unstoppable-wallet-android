package io.horizontalsystems.bankwallet.modules.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActivateTokenViewModel(
    wallet: Wallet,
    adapterManager: IAdapterManager,
) : ViewModelUiState<ActivateTokenUiState>() {
    private val token = wallet.token
    private val adapter = adapterManager.getReceiveAdapterForWallet(wallet)
    private val activateEnabled = true

    override fun createState() = ActivateTokenUiState(
        token = token,
        currency = App.currencyManager.baseCurrency,
        activateEnabled = activateEnabled
    )

    suspend fun activate() = withContext(Dispatchers.Default) {
        adapter?.activate()
    }

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ActivateTokenViewModel(wallet, App.adapterManager) as T
        }
    }
}

data class ActivateTokenUiState(
    val token: Token,
    val currency: Currency,
    val activateEnabled: Boolean
)
