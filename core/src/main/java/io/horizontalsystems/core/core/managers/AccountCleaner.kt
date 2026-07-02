package io.horizontalsystems.core.core.managers

import io.horizontalsystems.core.core.IAccountCleaner
import io.horizontalsystems.core.core.adapters.BitcoinAdapter
import io.horizontalsystems.core.core.adapters.BitcoinCashAdapter
import io.horizontalsystems.core.core.adapters.DashAdapter
import io.horizontalsystems.core.core.adapters.ECashAdapter
import io.horizontalsystems.core.core.adapters.Eip20Adapter
import io.horizontalsystems.core.core.adapters.EvmAdapter
import io.horizontalsystems.core.core.adapters.MoneroAdapter
import io.horizontalsystems.core.core.adapters.SolanaAdapter
import io.horizontalsystems.core.core.adapters.TronAdapter
import io.horizontalsystems.core.core.adapters.zcash.ZcashAdapter

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
        ZcashAdapter.clear(accountId)
        SolanaAdapter.clear(accountId)
        TronAdapter.clear(accountId)
        MoneroAdapter.clear(accountId)
    }

}
