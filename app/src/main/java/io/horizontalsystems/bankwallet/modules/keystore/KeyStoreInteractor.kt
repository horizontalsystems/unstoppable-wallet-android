package io.horizontalsystems.bankwallet.modules.keystore

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IKeyStoreManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.core.ISystemInfoManager

class KeyStoreInteractor(
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val localStorage: ILocalStorage,
        private val systemInfoManager: ISystemInfoManager,
        private val keyStoreManager: IKeyStoreManager)
    : KeyStoreModule.IInteractor {

    var delegate: KeyStoreModule.IInteractorDelegate? = null

    override val isSystemLockOff: Boolean
        get() = systemInfoManager.isSystemLockOff

    override val isKeyInvalidated: Boolean
        get() = keyStoreManager.isKeyInvalidated

    override val isUserNotAuthenticated: Boolean
        get() = keyStoreManager.isUserNotAuthenticated

    override fun resetApp() {
        accountManager.clear()
        walletManager.enable(listOf())
        localStorage.clear()
    }

    override fun removeKey() {
        keyStoreManager.removeKey()
    }

}
