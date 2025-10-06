package io.horizontalsystems.bankwallet.modules.main

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.walletconnect.web3.wallet.client.Wallet
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.DAppRequestEntityWrapper
import io.horizontalsystems.bankwallet.core.managers.TonConnectManager
import io.horizontalsystems.bankwallet.core.managers.UserManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate
import io.horizontalsystems.core.IKeyStoreManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.security.KeyStoreValidationError
import io.horizontalsystems.tonkit.models.SignTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainActivityViewModel(
    private val userManager: UserManager,
    private val accountManager: IAccountManager,
    private val pinComponent: IPinComponent,
    private val systemInfoManager: ISystemInfoManager,
    private val keyStoreManager: IKeyStoreManager,
    private val localStorage: ILocalStorage,
    private val tonConnectManager: TonConnectManager
) : ViewModel() {

    val navigateToMainLiveData = MutableLiveData(false)
    val wcEvent = MutableLiveData<Wallet.Model?>()
    val tcSendRequest = MutableLiveData<SignTransaction?>()
    val tcDappRequest = MutableLiveData<DAppRequestEntityWrapper?>()
    val intentLiveData = MutableLiveData<Intent?>()

    private val _contentHidden = MutableStateFlow(false)
    val contentHidden: StateFlow<Boolean> = _contentHidden.asStateFlow()

    init {
        viewModelScope.launch {
            userManager.currentUserLevelFlow.collect {
                navigateToMainLiveData.postValue(true)
            }
        }
        viewModelScope.launch {
            WCDelegate.walletEvents.collect {
                wcEvent.postValue(it)
            }
        }
        viewModelScope.launch {
            tonConnectManager.sendRequestFlow.collect {
                tcSendRequest.postValue(it)
            }
        }
        viewModelScope.launch {
            tonConnectManager.dappRequestFlow.collect {
                tcDappRequest.postValue(it)
            }
        }
        viewModelScope.launch {
            pinComponent.isLockedFlow.collect { locked ->
                _contentHidden.update { locked }
            }
        }
    }

    fun onWcEventHandled() {
        wcEvent.postValue(null)
    }

    fun onTcSendRequestHandled() {
        tcSendRequest.postValue(null)
    }

    fun onTcDappRequestHandled() {
        tcDappRequest.postValue(null)
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

        if (accountManager.isAccountsEmpty && !localStorage.mainShowedOnce) {
            throw MainScreenValidationError.Welcome()
        }
    }

    fun onNavigatedToMain() {
        navigateToMainLiveData.postValue(false)
    }

    fun setIntent(intent: Intent) {
        intentLiveData.postValue(intent)
    }

    fun intentHandled() {
        intentLiveData.postValue(null)
    }

    fun onResume() {
        _contentHidden.update { pinComponent.isLocked }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainActivityViewModel(
                App.userManager,
                App.accountManager,
                App.pinComponent,
                App.systemInfoManager,
                App.keyStoreManager,
                App.localStorage,
                App.tonConnectManager,
            ) as T
        }
    }
}

sealed class MainScreenValidationError : Exception() {
    class Welcome : MainScreenValidationError()
    class Unlock : MainScreenValidationError()
    class NoSystemLock : MainScreenValidationError()
    class KeyInvalidated : MainScreenValidationError()
    class UserAuthentication : MainScreenValidationError()
    class KeystoreRuntimeException : MainScreenValidationError()
}
