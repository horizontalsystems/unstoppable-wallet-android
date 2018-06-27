package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.DollarCurrency
import bitcoin.wallet.entities.WalletBalanceItem
import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.entities.coins.bitcoinCash.BitcoinCash

class WalletInteractor(private val databaseManager: IDatabaseManager) : WalletModule.IInteractor {

    var delegate: WalletModule.IInteractorDelegate? = null

    private var exchangeRates = mutableMapOf<String, Double>()
    private var totalValues = mutableMapOf<Coin, Double>()

    override fun notifyWalletBalances() {
        databaseManager.getBitcoinUnspentOutputs().subscribe {
            totalValues[Bitcoin()] = it.array.map { it.value }.sum() / 100000000.0

            refresh()
        }

        databaseManager.getBitcoinCashUnspentOutputs().subscribe {
            totalValues[BitcoinCash()] = it.array.map { it.value }.sum() / 100000000.0

            refresh()
        }

        databaseManager.getExchangeRates().subscribe {
            exchangeRates = it.array.associateBy({ it.code }, { it.value }).toMutableMap()

            refresh()
        }
    }

    private fun refresh() {
        val walletBalances = mutableListOf<WalletBalanceItem>()

        totalValues.forEach { totalValueEntry ->
            val coin = totalValueEntry.key
            val total = totalValueEntry.value

            exchangeRates[coin.code]?.let { rate ->
                walletBalances.add(WalletBalanceItem(CoinValue(coin, total), rate, DollarCurrency()))
            }
        }

        if (walletBalances.isNotEmpty()) {
            delegate?.didFetchWalletBalances(walletBalances)
        }
    }

}
