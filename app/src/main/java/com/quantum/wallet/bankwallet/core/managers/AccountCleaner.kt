package com.quantum.wallet.bankwallet.core.managers

import com.quantum.wallet.bankwallet.core.IAccountCleaner
import com.quantum.wallet.bankwallet.core.adapters.BitcoinAdapter
import com.quantum.wallet.bankwallet.core.adapters.BitcoinCashAdapter
import com.quantum.wallet.bankwallet.core.adapters.DashAdapter
import com.quantum.wallet.bankwallet.core.adapters.ECashAdapter
import com.quantum.wallet.bankwallet.core.adapters.Eip20Adapter
import com.quantum.wallet.bankwallet.core.adapters.EvmAdapter
import com.quantum.wallet.bankwallet.core.adapters.MoneroAdapter
import com.quantum.wallet.bankwallet.core.adapters.SolanaAdapter
import com.quantum.wallet.bankwallet.core.adapters.TronAdapter
import com.quantum.wallet.bankwallet.core.adapters.zcash.ZcashAdapter

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
