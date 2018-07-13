package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.DollarCurrency
import bitcoin.wallet.entities.Ethereum
import bitcoin.wallet.entities.WalletBalanceItem
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.entities.coins.bitcoinCash.BitcoinCash

class WalletInteractor(private val databaseManager: IDatabaseManager) : WalletModule.IInteractor {

    var delegate: WalletModule.IInteractorDelegate? = null

    private var exchangeRates = mutableMapOf<String, Double>()
    private var balances = mutableMapOf<String, Long>()

    override fun notifyWalletBalances() {
        databaseManager.getBalances().subscribe {
            balances = it.array.associateBy({ it.code }, { it.value }).toMutableMap()

            refresh()
        }

        databaseManager.getExchangeRates().subscribe {
            exchangeRates = it.array.associateBy({ it.code }, { it.value }).toMutableMap()

            refresh()
        }
    }

    private fun refresh() {
        val walletBalances = mutableListOf<WalletBalanceItem>()

        balances.forEach {
            val code = it.key
            val total = it.value

            val coin = when(code) {
                "BTC" -> Bitcoin()
                "BCH" -> BitcoinCash()
                "ETH" -> Ethereum()
                else -> null
            }

            if (coin != null) {
                exchangeRates[code]?.let { rate ->
                    walletBalances.add(WalletBalanceItem(CoinValue(coin, total / 100000000.0), rate, DollarCurrency()))
                }
            }
        }

        if (walletBalances.isNotEmpty()) {
            delegate?.didFetchWalletBalances(walletBalances)
        }
    }

}
