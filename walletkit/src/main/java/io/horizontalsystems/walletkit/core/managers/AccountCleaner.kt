package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.IAccountCleaner
import io.horizontalsystems.walletkit.core.adapters.BitcoinAdapter
import io.horizontalsystems.walletkit.core.adapters.BitcoinCashAdapter
import io.horizontalsystems.walletkit.core.adapters.DashAdapter
import io.horizontalsystems.walletkit.core.adapters.ECashAdapter
import io.horizontalsystems.walletkit.core.adapters.Eip20Adapter
import io.horizontalsystems.walletkit.core.adapters.EvmAdapter
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.adapters.TronAdapter

class AccountCleaner : IAccountCleaner {

    override fun clearAccounts(accountIds: List<String>) {
        accountIds.forEach { clearAccount(it) }
    }

    private fun clearAccount(accountId: String) {
        BitcoinAdapter.clear(accountId)
        BitcoinCashAdapter.clear(accountId)
        ECashAdapter.clear(accountId)
        DashAdapter.clear(accountId)
        EvmAdapter.clear(accountId)
        Eip20Adapter.clear(accountId)
        TronAdapter.clear(accountId)
        ChainRegistry.all.forEach { it.clearAccountData(accountId) }
    }

}
