package io.horizontalsystems.core.core.managers

import io.horizontalsystems.core.IKeyStoreCleaner
import io.horizontalsystems.core.core.IAccountManager
import io.horizontalsystems.core.core.ILocalStorage

class KeyStoreCleaner(
        private val localStorage: ILocalStorage,
        private val accountManager: IAccountManager,
        private val walletManager: WalletManager)
    : IKeyStoreCleaner {

    override var encryptedSampleText: String?
        get() = localStorage.encryptedSampleText
        set(value) {
            localStorage.encryptedSampleText = value
        }

    override fun cleanApp() {
        accountManager.clear()
        walletManager.clear()
        localStorage.clear()
    }
}
