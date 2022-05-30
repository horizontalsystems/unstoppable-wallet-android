package io.horizontalsystems.bankwallet.modules.launcher

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.core.IKeyStoreManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.security.KeyStoreValidationResult

class LaunchViewModel(
    private val accountManager: IAccountManager,
    private val pinComponent: IPinComponent,
    private val systemInfoManager: ISystemInfoManager,
    private val keyStoreManager: IKeyStoreManager,
    private val localStorage: ILocalStorage
) : ViewModel() {
    val torEnabled by localStorage::torEnabled

    private val mainShowedOnce = localStorage.mainShowedOnce

    fun getPage() = when {
        systemInfoManager.isSystemLockOff -> Page.NoSystemLock
        else -> when (keyStoreManager.validateKeyStore()) {
            KeyStoreValidationResult.UserNotAuthenticated -> Page.UserAuthentication
            KeyStoreValidationResult.KeyIsInvalid -> Page.KeyInvalidated
            KeyStoreValidationResult.KeyIsValid -> when {
                accountManager.isAccountsEmpty && !mainShowedOnce -> Page.Welcome
                pinComponent.isLocked -> Page.Unlock
                else -> Page.Main
            }
        }
    }

    enum class Page {
        Welcome,
        Main,
        Unlock,
        NoSystemLock,
        KeyInvalidated,
        UserAuthentication
    }
}
