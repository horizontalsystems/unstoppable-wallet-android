package io.horizontalsystems.bankwallet.modules.settings.support

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await

class PersonalSupportViewModel(
    private val marketKitWrapper: MarketKitWrapper,
    private val localStorage: ILocalStorage,
) : ViewModelUiState<PersonalSupportModule.UiState>() {

    private var contactName: String = ""
    private var showSuccess = false
    private var showError = false
    private var showSpinner = false
    private var buttonEnabled = false
    private var showRequestForm = !localStorage.personalSupportEnabled

    override fun createState() = PersonalSupportModule.UiState(
        contactName = contactName,
        showSuccess = showSuccess,
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

    fun successShown() {
        showSuccess = false
        emitState()
    }

    fun onRequestClicked() {
        showSpinner = true
        buttonEnabled = false
        emitState()

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
                emitState()
                return@launch
            }
            showSpinner = false
            buttonEnabled = true
            showSuccess = true
            emitState()
        }
    }

    fun showRequestForm() {
        showRequestForm = true
        emitState()
    }

}
