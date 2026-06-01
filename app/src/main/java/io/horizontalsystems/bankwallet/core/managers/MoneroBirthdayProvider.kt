package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.monerokit.MoneroKit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoneroBirthdayProvider @Inject constructor() {
    fun restoreHeightForNewWallet(): Long {
        return MoneroKit.restoreHeightForNewWallet()
    }
}
