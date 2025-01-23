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
    private var showRequestForm = !localStorage.vipSupportEnabled

    override fun createState() = VipSupportModule.UiState(
        contactName = contactName,
        showError = showError,
        showSpinner = showSpinner,
        buttonEnabled = buttonEnabled,
        showRequestForm = showRequestForm
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
                if (request.code() != 200) throw Exception("Error")
                localStorage.vipSupportEnabled = true
                showRequestForm = false
            } catch (e: Throwable) {
                showSpinner = false
                buttonEnabled = true
                showError = true
                emitState()
                return@launch
            }
            showSpinner = false
            buttonEnabled = true
            emitState()
        }
    }

    fun showRequestForm() {
        showRequestForm = true
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
        val showRequestForm: Boolean = false,
    )
}