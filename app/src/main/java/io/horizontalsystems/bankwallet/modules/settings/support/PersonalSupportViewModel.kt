package io.horizontalsystems.bankwallet.modules.settings.support

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await

class PersonalSupportViewModel(
    private val marketKitWrapper: MarketKitWrapper,
    private val localStorage: ILocalStorage,
) : ViewModel() {

    private var contactName: String = ""
    private var showSuccess = false
    private var showError = false
    private var showSpinner = false
    private var buttonEnabled = false
    private var showRequestForm = !localStorage.personalSupportEnabled

    var uiState by mutableStateOf(
        PersonalSupportModule.UiState(
            contactName = contactName,
            showSuccess = showSuccess,
            showError = showError,
            showSpinner = showSpinner,
            buttonEnabled = buttonEnabled,
            showRequestForm = showRequestForm
        )
    )
        private set

    fun onUsernameChange(username: String) {
        contactName = username
        buttonEnabled = username.isNotEmpty()
        syncState()
    }

    fun errorShown() {
        showError = false
        syncState()
    }

    fun successShown() {
        showSuccess = false
        syncState()
    }

    fun onRequestClicked() {
        showSpinner = true
        buttonEnabled = false
        syncState()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = marketKitWrapper.requestPersonalSupport(contactName).await()
                if (request.code() != 200) throw Exception("Error")
                localStorage.personalSupportEnabled = true
                showRequestForm = false
            } catch (e: Throwable) {
                showSpinner = false
                buttonEnabled = true
                showError = true
                syncState()
                return@launch
            }
            showSpinner = false
            buttonEnabled = true
            showSuccess = true
            syncState()
        }
    }

    private fun syncState() {
        viewModelScope.launch {
            uiState = PersonalSupportModule.UiState(
                contactName = contactName,
                showSuccess = showSuccess,
                showError = showError,
                showSpinner = showSpinner,
                buttonEnabled = buttonEnabled,
                showRequestForm = showRequestForm
            )
        }
    }

    fun showRequestForm() {
        showRequestForm = true
        syncState()
    }

}
