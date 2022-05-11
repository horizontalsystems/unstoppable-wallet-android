package io.horizontalsystems.bankwallet.modules.profeatures.yakauthorization

import io.horizontalsystems.bankwallet.core.toRawHexString
import io.horizontalsystems.bankwallet.modules.profeatures.HSProFeaturesAdapter
import io.horizontalsystems.bankwallet.modules.profeatures.ProFeaturesAuthorizationManager
import io.horizontalsystems.bankwallet.modules.profeatures.ProNft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class YakAuthorizationService(
    val manager: ProFeaturesAuthorizationManager,
    val adapter: HSProFeaturesAdapter
) {

    sealed class State {
        object Idle : State()
        object Loading : State()
        object NoYakNft : State()
        object SessionKeyReceived : State()

        class MessageReceived(val accountData: ProFeaturesAuthorizationManager.AccountData, val message: String) : State()
        class Failed(val exception: Exception) : State()
    }

    private val _stateFlow = MutableStateFlow<State>(State.Idle)
    val stateFlow = _stateFlow.asStateFlow()

    suspend fun authenticate() = withContext(Dispatchers.IO) {
        _stateFlow.update { State.Loading }

        try {
            val nftHolderAccountData = manager.getNFTHolderAccountData(ProNft.YAK)
            if (nftHolderAccountData != null) {
                val message = adapter.getMessage(nftHolderAccountData.address.hex)

                _stateFlow.update { State.MessageReceived(nftHolderAccountData, message) }
            } else {
                _stateFlow.update { State.NoYakNft }
            }
        } catch (e: Exception) {
            _stateFlow.update { State.Failed(e) }
        }
    }

    suspend fun signConfirmed() = withContext(Dispatchers.IO) {
        val messageReceivedState = _stateFlow.value as? State.MessageReceived ?: return@withContext

        try {
            val signature = manager.signMessage(messageReceivedState.accountData, messageReceivedState.message)

            val key = adapter.authenticate(messageReceivedState.accountData.address.hex, "0x${signature.toRawHexString()}")
            manager.saveSessionKey(ProNft.YAK, messageReceivedState.accountData, key)

            _stateFlow.update { State.SessionKeyReceived }
        } catch (e: Exception) {
            _stateFlow.update { State.Failed(e) }
        }
    }

    fun authorizationCanceled() {
        _stateFlow.update { State.Idle }
    }

}
