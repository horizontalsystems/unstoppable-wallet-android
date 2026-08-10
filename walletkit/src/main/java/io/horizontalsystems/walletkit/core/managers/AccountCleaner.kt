package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.IAccountCleaner
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.adapters.TronAdapter

class AccountCleaner : IAccountCleaner {

    override fun clearAccounts(accountIds: List<String>) {
        accountIds.forEach { clearAccount(it) }
    }

    private fun clearAccount(accountId: String) {
        TronAdapter.clear(accountId)
        ChainRegistry.all.forEach { it.clearAccountData(accountId) }
    }

}
