package cash.p.terminal.modules.subscription

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ActivateSubscriptionViewModel(private val address: String) : ViewModel() {

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

    }


    fun cancel() {
        TODO("Not yet implemented")
    }

    fun fetchMessageToSign() {
        TODO("Not yet implemented")
    }

    fun sign() {
        TODO("Not yet implemented")
    }

    class Factory(private val address: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ActivateSubscriptionViewModel(address) as T
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
