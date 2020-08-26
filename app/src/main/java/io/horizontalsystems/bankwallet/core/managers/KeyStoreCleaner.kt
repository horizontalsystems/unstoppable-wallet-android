package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.core.IKeyStoreCleaner
import io.horizontalsystems.core.IPinStorage

class KeyStoreCleaner(
        private val localStorage: ILocalStorage,
        private val pinStorage: IPinStorage,
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager)
    : IKeyStoreCleaner {

    override var encryptedSampleText: String?
        get() = localStorage.encryptedSampleText
        set(value) {
            localStorage.encryptedSampleText = value
        }

    override fun cleanApp() {
        pinStorage.clearPin()
        accountManager.clear()
        walletManager.enable(listOf())
        localStorage.clear()
    }
}
