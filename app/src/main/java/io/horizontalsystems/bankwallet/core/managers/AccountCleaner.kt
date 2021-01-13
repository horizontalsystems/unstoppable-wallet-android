package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountCleaner
import io.horizontalsystems.bankwallet.core.adapters.*
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.entities.CoinType

class AccountCleaner(private val testMode: Boolean) : IAccountCleaner {

    override fun clearAccounts(accountIds: List<String>) {
        accountIds.forEach { clearAccount(it) }
    }

    override fun clearAccount(coinType: CoinType, accountId: String) {
        when (coinType) {
            CoinType.Bitcoin -> BitcoinAdapter.clear(accountId, testMode)
            CoinType.BitcoinCash -> BitcoinCashAdapter.clear(accountId, testMode)
            CoinType.Dash -> DashAdapter.clear(accountId, testMode)
            CoinType.Ethereum -> EthereumAdapter.clear(accountId, testMode)
            is CoinType.Erc20 -> Erc20Adapter.clear(accountId, testMode)
            is CoinType.Binance -> BinanceAdapter.clear(accountId, testMode)
            CoinType.Zcash -> ZcashAdapter.clear(accountId)
        }
    }

    private fun clearAccount(accountId: String) {
        BinanceAdapter.clear(accountId, testMode)
        BitcoinAdapter.clear(accountId, testMode)
        BitcoinCashAdapter.clear(accountId, testMode)
        DashAdapter.clear(accountId, testMode)
        EthereumAdapter.clear(accountId, testMode)
        Erc20Adapter.clear(accountId, testMode)
        ZcashAdapter.clear(accountId)
    }

}
