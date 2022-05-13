package io.horizontalsystems.keystore

import io.horizontalsystems.core.IKeyStoreManager

class KeyStoreInteractor(private val keyStoreManager: IKeyStoreManager)
    : KeyStoreModule.IInteractor {

    var delegate: KeyStoreModule.IInteractorDelegate? = null

    override fun resetApp(reason: String) {
        keyStoreManager.resetApp(reason)
    }

    override fun removeKey() {
        keyStoreManager.removeKey()
    }
}
