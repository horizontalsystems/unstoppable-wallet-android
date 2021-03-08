package io.horizontalsystems.bankwallet.modules.enablecoins

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.binancechainkit.core.api.BinanceChainApi
import io.horizontalsystems.binancechainkit.models.Balance
import io.horizontalsystems.binancechainkit.models.Bep2Token
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.IBuildConfigProvider
import io.reactivex.Single
import java.math.BigDecimal

class EnableCoinsBep2Provider(appConfigProvider: IBuildConfigProvider, private val coinManager: ICoinManager) {

    private val networkType = if (appConfigProvider.testMode)
        BinanceChainKit.NetworkType.TestNet else
        BinanceChainKit.NetworkType.MainNet

    private val binanceApi = BinanceChainApi(networkType)

    fun getCoinsAsync(words: List<String>): Single<List<Coin>> {
        val address = BinanceChainKit.wallet(words, networkType).address

        return binanceApi.getBalances(address)
                .flatMap { balances ->
                    val nonZeroBalanceTokenSymbols = getSymbolsWithNonZeroBalance(balances)

                    val listedCoins = mutableListOf<Coin>()
                    val nonListedCoinSymbols = mutableListOf<String>()

                    nonZeroBalanceTokenSymbols.forEach {
                        if (it != "BNB") {
                            coinManager.getCoin(CoinType.Bep2(it))?.let { listedCoin ->
                                listedCoins.add(listedCoin)
                            } ?: kotlin.run {
                                nonListedCoinSymbols.add(it)
                            }
                        }
                    }

                    if (nonListedCoinSymbols.isEmpty()) {
                        return@flatMap Single.just(listedCoins)
                    }

                    binanceApi.getTokens()
                            .flatMap { bep2Tokens ->
                                val nonListedCoins = getCoins(nonListedCoinSymbols, bep2Tokens)
                                Single.just(listedCoins + nonListedCoins)
                            }
                }
    }

    private fun getCoins(nonListedCoinSymbols: MutableList<String>, bep2Tokens: List<Bep2Token>) : List<Coin> {
        return nonListedCoinSymbols.mapNotNull { symbol ->
            bep2Tokens.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }?.let { tokenInfo ->
                Coin(
                        title = tokenInfo.name,
                        code = tokenInfo.code,
                        decimal = 8,
                        type = CoinType.Bep2(tokenInfo.symbol)
                )
            }
        }
    }

    private fun getSymbolsWithNonZeroBalance(balances: List<Balance>): List<String> {
        return balances.mapNotNull { balance ->
            if (balance.amount > BigDecimal.ZERO) {
                balance.symbol
            } else {
                null
            }
        }
    }
}
