package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.adapters.*
import io.horizontalsystems.bankwallet.core.storage.AccountRecord

class AccountCleaner(private val testMode: Boolean) {

    fun clearAccounts(accountRecords: List<AccountRecord>) {
        accountRecords.forEach { clearAccount(it) }
    }

    private fun clearAccount(accountRecord: AccountRecord) {
        BinanceAdapter.clear(accountRecord.id, testMode)
        BitcoinAdapter.clear(accountRecord.id, testMode)
        BitcoinCashAdapter.clear(accountRecord.id, testMode)
        DashAdapter.clear(accountRecord.id, testMode)
        EosAdapter.clear(accountRecord.id, testMode)
        EthereumAdapter.clear(accountRecord.id, testMode)
        Erc20Adapter.clear(accountRecord.id, testMode)
    }

}
