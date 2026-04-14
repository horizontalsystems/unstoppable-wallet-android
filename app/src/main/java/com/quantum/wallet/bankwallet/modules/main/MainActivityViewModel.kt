package com.quantum.wallet.bankwallet.modules.main

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reown.walletkit.client.Wallet
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.IAccountManager
import com.quantum.wallet.bankwallet.core.ILocalStorage
import com.quantum.wallet.bankwallet.core.managers.DAppRequestEntityWrapper
import com.quantum.wallet.bankwallet.core.managers.TonConnectManager
import com.quantum.wallet.bankwallet.core.managers.UserManager
import com.quantum.wallet.bankwallet.modules.walletconnect.WCDelegate
import com.quantum.wallet.core.IKeyStoreManager
import com.quantum.wallet.core.ISystemInfoManager
import com.quantum.wallet.core.security.KeyStoreValidationError
import io.horizontalsystems.tonkit.models.SignTransaction
import kotlinx.coroutines.launch

class MainActivityViewModel(
    private val userManager: UserManager,
    private val accountManager: IAccountManager,
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
    }

    fun onWcEventHandled() {
        wcEvent.postValue(null)
    }

    fun reEmitPendingWcProposalIfNeeded() {
        if (wcEvent.value == null && WCDelegate.sessionProposalEvent != null) {
            wcEvent.postValue(WCDelegate.sessionProposalEvent!!.first)
        }
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

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainActivityViewModel(
                App.userManager,
                App.accountManager,
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
