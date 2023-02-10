package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountCleaner
import io.horizontalsystems.bankwallet.core.adapters.*
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter

class AccountCleaner : IAccountCleaner {

    override fun clearAccounts(accountIds: List<String>) {
        accountIds.forEach { clearAccount(it) }
    }

    private fun clearAccount(accountId: String) {
        BinanceAdapter.clear(accountId)
        BitcoinAdapter.clear(accountId)
        BitcoinCashAdapter.clear(accountId)
        DashAdapter.clear(accountId)
        EvmAdapter.clear(accountId)
        Eip20Adapter.clear(accountId)
        ZcashAdapter.clear(accountId)
        SolanaAdapter.clear(accountId)
    }

}
