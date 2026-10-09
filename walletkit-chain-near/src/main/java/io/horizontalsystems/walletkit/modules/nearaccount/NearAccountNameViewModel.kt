package io.horizontalsystems.walletkit.modules.nearaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.nearkit.NearKit
import io.horizontalsystems.nearkit.crypto.PublicKey
import io.horizontalsystems.nearkit.network.Network
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.chain.near.NearChainPlugin
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.DataState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.URL

/** Checks that an account the user typed exists and that the recovery phrase's key has full access to it. */
class NearAccountNameViewModel(
    private val publicKey: PublicKey,
    private val rpcUrls: List<URL>,
    private val fastNearApiKey: String?,
) : ViewModelUiState<NearAccountNameUiState>() {

    private var input = ""
    private var state: DataState<Unit>? = null
    private var accountId: String? = null
    private var checkJob: Job? = null

    override fun createState() = NearAccountNameUiState(
        state = state,
        addEnabled = input.isNotEmpty() && state?.loading != true,
        accountId = accountId,
    )

    fun onEnterText(text: String) {
        input = text.trim().lowercase()
        checkJob?.cancel()
        state = null
        emitState()
    }

    fun onAdd() {
        val candidate = input
        if (!NearKit.isValidAccountId(candidate)) {
            state = error(R.string.NearAccountName_Invalid)
            emitState()
            return
        }

        state = DataState.Loading
        emitState()

        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            state = try {
                when {
                    !NearKit.accountState(candidate, Network.MainNet, fastNearApiKey, rpcUrls).exists ->
                        error(R.string.NearAccountName_NotFound)

                    !NearKit.hasFullAccess(candidate, publicKey, Network.MainNet, fastNearApiKey, rpcUrls) ->
                        error(R.string.NearAccountName_NoAccess)

                    else -> {
                        accountId = candidate
                        DataState.Success(Unit)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                error(R.string.NearAccountName_CheckFailed)
            }
            emitState()
        }
    }

    private fun error(textRes: Int) = DataState.Error(Exception(Translator.getString(textRes)))

    class Factory(private val publicKey: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val plugin = requireNotNull(ChainRegistry[BlockchainType.Near] as? NearChainPlugin) {
                "NEAR plugin is not registered"
            }
            return NearAccountNameViewModel(
                PublicKey.fromString(publicKey),
                plugin.rpcSourceManager.rpcUrls(),
                App.appConfigProvider.fastNearApiKey,
            ) as T
        }
    }
}

data class NearAccountNameUiState(
    val state: DataState<Unit>?,
    val addEnabled: Boolean,
    val accountId: String?,
)
