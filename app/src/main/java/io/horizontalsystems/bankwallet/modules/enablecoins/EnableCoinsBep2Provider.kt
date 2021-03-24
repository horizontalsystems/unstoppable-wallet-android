package io.horizontalsystems.bankwallet.modules.enablecoins

import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.binancechainkit.core.api.BinanceChainApi
import io.horizontalsystems.core.IBuildConfigProvider
import io.reactivex.Single
import java.math.BigDecimal

class EnableCoinsBep2Provider(appConfigProvider: IBuildConfigProvider) {

    private val networkType = if (appConfigProvider.testMode)
        BinanceChainKit.NetworkType.TestNet else
        BinanceChainKit.NetworkType.MainNet

    private val binanceApi = BinanceChainApi(networkType)

    fun getTokenSymbolsAsync(words: List<String>): Single<List<String>> {
        val address = BinanceChainKit.wallet(words, networkType).address

        return binanceApi.getBalances(address)
                .map { balances ->
                    balances.mapNotNull { balance ->
                        if (balance.amount > BigDecimal.ZERO)
                            balance.symbol
                        else
                            null
                    }.distinct()
                }
    }

}
