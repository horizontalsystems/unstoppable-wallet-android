package io.horizontalsystems.core.core.managers

import io.horizontalsystems.monerokit.MoneroKit

class MoneroBirthdayProvider {
    fun restoreHeightForNewWallet(): Long {
        return MoneroKit.restoreHeightForNewWallet()
    }
}
