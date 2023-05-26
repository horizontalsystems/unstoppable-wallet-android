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
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await

class ActivateSubscriptionViewModel(
    private val address: String,
    private val marketKit: MarketKitWrapper,
    private val accountManager: IAccountManager
) : ViewModel() {

    private val account = accountManager.accounts.find {
        it.type.evmAddress(App.evmBlockchainManager.getChain(BlockchainType.Ethereum))?.hex == address
    }
    private var fetchingMessage = true
    private var fetchingMessageError: Throwable? = null
    private var subscriptionInfo: SubscriptionInfo? = null

    var uiState: ActivateSubscription by mutableStateOf(
        ActivateSubscription(
            fetchingMessage = fetchingMessage,
            fetchingMessageError = fetchingMessageError,
            subscriptionInfo = subscriptionInfo
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
                    walletName = "walletName",
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
                subscriptionInfo = subscriptionInfo
            )
        }
    }


    fun cancel() {
        TODO("Not yet implemented")
    }

    fun sign() {
        val tmpSubscriptionInfo = subscriptionInfo ?: return
        val tmpAccount = account ?: return

        val signature = tmpAccount.type.sign(tmpSubscriptionInfo.messageToSign.toByteArray()) ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val token = marketKit.authenticate(signature.toHexString(), address).await()
        }
    }

    class Factory(private val address: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ActivateSubscriptionViewModel(
                address,
                App.marketKit,
                App.accountManager
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
    val subscriptionInfo: SubscriptionInfo?
) {
//    sealed class Step {
//        object FetchingMessageToSign : Step()
//        object FetchingMessageToSignFailed : Step()
//        data class FetchingMessageToSignSuccess(
//            val walletName: String,
//            val walletAddress: String,
//            val messageToSign: String
//        ) : Step()
//
//        data class SendingSignedMessage(
//            val walletName: String,
//            val walletAddress: String,
//            val messageToSign: String
//        ) : Step()
//
//        object SendingSignedMessageFailed : Step()
//        object SendingSignedMessageSuccess : Step()
//    }
}
