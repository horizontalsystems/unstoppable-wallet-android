package io.horizontalsystems.walletkit.modules.main

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.IKeyStoreManager
import io.horizontalsystems.walletkit.ISystemInfoManager
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.managers.UserManager
import io.horizontalsystems.walletkit.modules.walletconnect.WCDelegate
import io.horizontalsystems.walletkit.security.KeyStoreValidationError
import io.horizontalsystems.dapp.core.HSDAppEvent
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivityViewModel(
    private val userManager: UserManager,
    private val accountManager: IAccountManager,
    private val systemInfoManager: ISystemInfoManager,
    private val keyStoreManager: IKeyStoreManager,
    private val localStorage: ILocalStorage
) : ViewModelUiState<MainUIState>() {

    val navigateToMainLiveData = MutableLiveData<String?>(null)
    val wcEvent = MutableLiveData<HSDAppEvent?>()
    val intentLiveData = MutableLiveData<Intent?>()

    var mainShowedOnce = localStorage.mainShowedOnceFlow.value

    override fun createState() = MainUIState(
        mainShowedOnce = mainShowedOnce
    )

    init {
        viewModelScope.launch {
            localStorage.mainShowedOnceFlow.collect {
                mainShowedOnce = it
                emitState()
            }
        }

        viewModelScope.launch {
            // Only a change of user level sends the app back to the main screen. The StateFlow's
            // current value replays on every activity start and would pop a page opened on
            // launch (a widget deeplink, or a restored back stack).
            userManager.currentUserLevelFlow.drop(1).collect {
                navigateToMainLiveData.postValue(UUID.randomUUID().toString())
            }
        }
        viewModelScope.launch {
            WCDelegate.walletEvents.collect {
                wcEvent.value = it
            }
        }
    }

    fun onWcEventHandled() {
        wcEvent.value = null
    }

    fun reEmitPendingWcEventIfNeeded() {
        if (wcEvent.value != null) return

        WCDelegate.sessionRequestEvent?.let {
            wcEvent.value = HSDAppEvent.SessionRequest(it)
            return
        }
        WCDelegate.sessionProposalEvent?.let {
            wcEvent.value = HSDAppEvent.SessionProposal(it)
        }
    }

    fun validate() {
        if (systemInfoManager.isSystemLockOff) {
            throw MainScreenValidationError.NoSystemLock()
        }

        try {
            keyStoreManager.validateKeyStore()
        } catch (e: KeyStoreValidationError.UserNotAuthenticated) {
            throw MainScreenValidationError.UserAuthentication()
        } catch (e: KeyStoreValidationError.KeyIsInvalid) {
            throw MainScreenValidationError.KeyInvalidated()
        } catch (e: RuntimeException) {
            throw MainScreenValidationError.KeystoreRuntimeException()
        }
    }

    fun onNavigatedToMain() {
        navigateToMainLiveData.postValue(null)
    }

    fun setIntent(intent: Intent) {
        intentLiveData.postValue(intent)
    }

    fun intentHandled() {
        intentLiveData.postValue(null)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainActivityViewModel(
                App.userManager,
                App.accountManager,
                App.systemInfoManager,
                App.keyStoreManager,
                App.localStorage,
            ) as T
        }
    }
}

data class MainUIState(val mainShowedOnce: Boolean)

sealed class MainScreenValidationError : Exception() {
    class Unlock : MainScreenValidationError()
    class NoSystemLock : MainScreenValidationError()
    class KeyInvalidated : MainScreenValidationError()
    class UserAuthentication : MainScreenValidationError()
    class KeystoreRuntimeException : MainScreenValidationError()
}
