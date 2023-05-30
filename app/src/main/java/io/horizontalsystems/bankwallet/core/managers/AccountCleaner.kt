package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountCleaner
import io.horizontalsystems.bankwallet.core.adapters.BinanceAdapter
import io.horizontalsystems.bankwallet.core.adapters.BitcoinAdapter
import io.horizontalsystems.bankwallet.core.adapters.BitcoinCashAdapter
import io.horizontalsystems.bankwallet.core.adapters.DashAdapter
import io.horizontalsystems.bankwallet.core.adapters.ECashAdapter
import io.horizontalsystems.bankwallet.core.adapters.Eip20Adapter
import io.horizontalsystems.bankwallet.core.adapters.EvmAdapter
import io.horizontalsystems.bankwallet.core.adapters.SolanaAdapter
import io.horizontalsystems.bankwallet.core.adapters.TronAdapter
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter

class AccountCleaner : IAccountCleaner {

    override fun clearAccounts(accountIds: List<String>) {
        accountIds.forEach { clearAccount(it) }
    }

    private fun clearAccount(accountId: String) {
        BinanceAdapter.clear(accountId)
        BitcoinAdapter.clear(accountId)
        BitcoinCashAdapter.clear(accountId)
        ECashAdapter.clear(accountId)
        DashAdapter.clear(accountId)
        EvmAdapter.clear(accountId)
        Eip20Adapter.clear(accountId)
        ZcashAdapter.clear(accountId)
        SolanaAdapter.clear(accountId)
        TronAdapter.clear(accountId)
    }

}
