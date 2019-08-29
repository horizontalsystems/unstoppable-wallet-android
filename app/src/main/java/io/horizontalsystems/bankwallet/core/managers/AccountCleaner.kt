package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.adapters.*

class AccountCleaner(private val testMode: Boolean) {

    fun clearAccounts(accountRecords: List<String>) {
        accountRecords.forEach { clearAccount(it) }
    }

    private fun clearAccount(accountId: String) {
        BinanceAdapter.clear(accountId, testMode)
        BitcoinAdapter.clear(accountId, testMode)
        BitcoinCashAdapter.clear(accountId, testMode)
        DashAdapter.clear(accountId, testMode)
        EosAdapter.clear(accountId, testMode)
        EthereumAdapter.clear(accountId, testMode)
        Erc20Adapter.clear(accountId, testMode)
    }

}
