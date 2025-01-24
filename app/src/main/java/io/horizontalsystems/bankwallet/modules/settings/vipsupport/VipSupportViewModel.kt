package io.horizontalsystems.bankwallet.modules.settings.vipsupport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await

class VipSupportViewModel(
    private val marketKitWrapper: MarketKitWrapper,
    private val localStorage: ILocalStorage,
) : ViewModelUiState<VipSupportModule.UiState>() {

    private var contactName: String = ""
    private var showError = false
    private var showSpinner = false
    private var buttonEnabled = false
    private var telegramGroupLink: String? = localStorage.vipSupportLink

    override fun createState() = VipSupportModule.UiState(
        contactName = contactName,
        showError = showError,
        showSpinner = showSpinner,
        buttonEnabled = buttonEnabled,
        vipSupportLink = telegramGroupLink
    )

    fun onUsernameChange(username: String) {
        contactName = username
        buttonEnabled = username.isNotEmpty()
        emitState()
    }

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
                val request = marketKitWrapper.requestVipSupport(contactName).await()
                val groupLink = request["group_link"]
                if (groupLink == null) {
                    throw Exception("Error")
                } else {
                    localStorage.vipSupportLink = groupLink
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

    fun showRequestForm() {
        telegramGroupLink = null
        localStorage.vipSupportLink = null
        emitState()
    }

}

object VipSupportModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VipSupportViewModel(App.marketKit, App.localStorage) as T
        }
    }

    data class UiState(
        val contactName: String,
        val showError: Boolean = false,
        val showSpinner: Boolean = false,
        val buttonEnabled: Boolean = false,
        val vipSupportLink: String? = null,
    )
}