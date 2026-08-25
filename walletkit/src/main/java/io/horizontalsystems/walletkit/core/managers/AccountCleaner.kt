package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.IAccountCleaner
import io.horizontalsystems.walletkit.core.chain.ChainRegistry

class AccountCleaner : IAccountCleaner {

    override fun clearAccounts(accountIds: List<String>) {
        accountIds.forEach { clearAccount(it) }
    }

    private fun clearAccount(accountId: String) {
        ChainRegistry.all.forEach { it.clearAccountData(accountId) }
    }

}
