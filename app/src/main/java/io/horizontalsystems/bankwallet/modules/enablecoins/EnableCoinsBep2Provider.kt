package io.horizontalsystems.bankwallet.modules.enablecoins

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.binancechainkit.core.api.BinanceChainApi
import io.horizontalsystems.binancechainkit.core.api.BinanceError
import io.horizontalsystems.binancechainkit.models.Balance
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

        return Single.zip(
                binanceApi.getBalances(address),
                binanceApi.getTokens(),
                { symbols, bep2Tokens -> Pair(symbols, bep2Tokens) })

                .onErrorResumeNext {
                    if ((it as? BinanceError)?.code == 404) {
                        Single.just(Pair(listOf(), listOf())) // New Account
                    } else {
                        Single.error(it.fillInStackTrace())
                    }
                }
                .flatMap { (balances, bep2Tokens) ->
                    val nonZeroBalanceTokenSymbols = getSymbolsWithNonZeroBalance(balances)
                    val listedTokens = mutableListOf<Coin>()
                    val nonListedTokenSymbols = mutableListOf<String>()

                    nonZeroBalanceTokenSymbols.forEach {
                        if (it != "BNB") {
                            coinManager.getCoin(CoinType.Bep2(it))?.let { listedCoin ->
                                listedTokens.add(listedCoin)
                            } ?: kotlin.run {
                                nonListedTokenSymbols.add(it)
                            }
                        }
                    }

                    if (nonListedTokenSymbols.isEmpty()){
                        return@flatMap Single.just(listedTokens)
                    }

                    val nonListedCoins = nonListedTokenSymbols.mapNotNull { symbol ->
                        bep2Tokens.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }?.let { token ->
                            Coin(
                                    title = token.name,
                                    code = token.code,
                                    decimal = 8,
                                    type = CoinType.Bep2(token.symbol)
                            )
                        }
                    }

                    return@flatMap Single.just(listedTokens + nonListedCoins)
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
