package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.IAccountCleaner
import io.horizontalsystems.walletkit.core.adapters.Eip20Adapter
import io.horizontalsystems.walletkit.core.adapters.EvmAdapter
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.adapters.TronAdapter

class AccountCleaner : IAccountCleaner {

    override fun clearAccounts(accountIds: List<String>) {
        accountIds.forEach { clearAccount(it) }
    }

    private fun clearAccount(accountId: String) {
        EvmAdapter.clear(accountId)
        Eip20Adapter.clear(accountId)
        TronAdapter.clear(accountId)
        ChainRegistry.all.forEach { it.clearAccountData(accountId) }
    }

}
