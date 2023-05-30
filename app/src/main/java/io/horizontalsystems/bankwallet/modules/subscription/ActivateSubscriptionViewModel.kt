package io.horizontalsystems.bankwallet.modules.subscription

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.SubscriptionManager
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v1.WCSessionViewModel
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await

class ActivateSubscriptionViewModel(
    private val address: String,
    private val marketKit: MarketKitWrapper,
    private val accountManager: IAccountManager,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    private val account = accountManager.accounts.find {
        it.type.evmAddress(App.evmBlockchainManager.getChain(BlockchainType.Ethereum))?.hex == address
    }
    private var fetchingMessage = true
    private var fetchingMessageError: Throwable? = null
    private var subscriptionInfo: SubscriptionInfo? = null
    private var fetchingToken = false
    private var fetchingTokenError: Throwable? = null
    private var fetchingTokenSuccess = false

    var uiState: ActivateSubscription by mutableStateOf(
        ActivateSubscription(
            fetchingMessage = fetchingMessage,
            fetchingMessageError = fetchingMessageError,
            subscriptionInfo = subscriptionInfo,
            fetchingToken = fetchingToken,
            fetchingTokenError = fetchingTokenError,
            fetchingTokenSuccess = fetchingTokenSuccess,
            signButtonState = getSignButtonState(),
        )
    )
        private set

    init {
        fetchMessageToSign()
    }

    fun fetchMessageToSign() {
        viewModelScope.launch(Dispatchers.IO) {
            fetchingMessage = true
            emitState()

            try {
                val messageToSign = marketKit.authKey(address).await()

                fetchingMessage = false
                fetchingMessageError = null
                subscriptionInfo = SubscriptionInfo(
                    walletName = account?.name ?: "--",
                    walletAddress = address,
                    messageToSign = messageToSign
                )

            } catch (e: Throwable) {
                fetchingMessage = false
                fetchingMessageError = e
            }

            emitState()
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = ActivateSubscription(
                fetchingMessage = fetchingMessage,
                fetchingMessageError = fetchingMessageError,
                subscriptionInfo = subscriptionInfo,
                fetchingToken = fetchingToken,
                fetchingTokenError = fetchingTokenError,
                fetchingTokenSuccess = fetchingTokenSuccess,
                signButtonState = getSignButtonState()
            )
        }
    }

    private fun getSignButtonState() = when {
        fetchingToken -> WCSessionViewModel.ButtonState.Disabled
        fetchingMessage -> WCSessionViewModel.ButtonState.Hidden
        subscriptionInfo != null -> WCSessionViewModel.ButtonState.Enabled
        else -> WCSessionViewModel.ButtonState.Hidden
    }

    fun sign() {
        val tmpSubscriptionInfo = subscriptionInfo ?: return
        val tmpAccount = account ?: return

        fetchingToken = true
        emitState()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val signature = tmpAccount.type.sign(tmpSubscriptionInfo.messageToSign.toByteArray()) ?: throw IllegalStateException()
                val token = marketKit.authenticate(signature.toHexString(), address).await()
                subscriptionManager.authToken = token
                fetchingTokenSuccess = true
            } catch (t: Throwable) {
                fetchingTokenError = t
            }

            fetchingToken = false
            emitState()
        }
    }

    class Factory(private val address: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ActivateSubscriptionViewModel(
                address,
                App.marketKit,
                App.accountManager,
                App.subscriptionManager
            ) as T
        }
    }
}

data class SubscriptionInfo(
    val walletName: String,
    val walletAddress: String,
    val messageToSign: String
)

data class ActivateSubscription(
    val fetchingMessage: Boolean,
    val fetchingMessageError: Throwable?,
    val subscriptionInfo: SubscriptionInfo?,
    val fetchingToken: Boolean,
    val fetchingTokenError: Throwable?,
    val fetchingTokenSuccess: Boolean,
    val signButtonState: WCSessionViewModel.ButtonState
)
