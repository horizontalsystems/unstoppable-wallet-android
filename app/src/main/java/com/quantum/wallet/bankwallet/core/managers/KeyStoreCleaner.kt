package com.quantum.wallet.bankwallet.core.managers

import com.quantum.wallet.bankwallet.core.IAccountManager
import com.quantum.wallet.bankwallet.core.ILocalStorage
import com.quantum.wallet.core.IKeyStoreCleaner

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
