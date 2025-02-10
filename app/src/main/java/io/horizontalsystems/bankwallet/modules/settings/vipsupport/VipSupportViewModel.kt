package io.horizontalsystems.bankwallet.modules.settings.vipsupport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await

class VipSupportViewModel(
    private val marketKitWrapper: MarketKitWrapper,
) : ViewModelUiState<VipSupportModule.UiState>() {

    private var showError = false
    private var showSpinner = false
    private var telegramGroupLink: String? = null
    private var buttonEnabled = true

    override fun createState() = VipSupportModule.UiState(
        showError = showError,
        showSpinner = showSpinner,
        buttonEnabled = buttonEnabled,
        openTelegramGroup = telegramGroupLink
    )

    fun errorShown() {
        showError = false
        emitState()
    }

    fun onRequestClicked() {
        showSpinner = true
        buttonEnabled = false
        emitState()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val purchaseToken = UserSubscriptionManager.activeSubscriptionStateFlow.value?.purchaseToken
                    ?: throw Exception("No Purchase Token")

                val request = marketKitWrapper.requestVipSupport(purchaseToken).await()
                val groupLink = request["group_link"]
                if (groupLink == null) {
                    throw Exception("Error")
                } else {
                    telegramGroupLink = groupLink
                    showSpinner = false
                    buttonEnabled = true
                    emitState()
                }
            } catch (e: Throwable) {
                showSpinner = false
                buttonEnabled = true
                showError = true
                emitState()
                return@launch
            }
        }
    }

    fun telegramGroupOpened() {
        telegramGroupLink = null
        emitState()
    }

}

object VipSupportModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VipSupportViewModel(App.marketKit) as T
        }
    }

    data class UiState(
        val showError: Boolean = false,
        val showSpinner: Boolean = false,
        val buttonEnabled: Boolean = false,
        val openTelegramGroup: String? = null,
    )
}