package io.horizontalsystems.keystore

import io.horizontalsystems.core.IKeyStoreManager
import io.horizontalsystems.core.ISystemInfoManager

class KeyStoreInteractor(private val systemInfoManager: ISystemInfoManager, private val keyStoreManager: IKeyStoreManager)
    : KeyStoreModule.IInteractor {

    var delegate: KeyStoreModule.IInteractorDelegate? = null

    override val isSystemLockOff: Boolean
        get() = systemInfoManager.isSystemLockOff

    override val isKeyInvalidated: Boolean
        get() = keyStoreManager.isKeyInvalidated

    override val isUserNotAuthenticated: Boolean
        get() = keyStoreManager.isUserNotAuthenticated

    override fun resetApp() {
        keyStoreManager.resetApp()
    }

    override fun removeKey() {
        keyStoreManager.removeKey()
    }
}
