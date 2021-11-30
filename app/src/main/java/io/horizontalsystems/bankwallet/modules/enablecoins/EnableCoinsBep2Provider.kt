package io.horizontalsystems.bankwallet.modules.enablecoins

import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.binancechainkit.core.api.BinanceChainApi
import io.reactivex.Single
import java.math.BigDecimal

class EnableCoinsBep2Provider(testMode: Boolean) {

    private val networkType = if (testMode)
        BinanceChainKit.NetworkType.TestNet else
        BinanceChainKit.NetworkType.MainNet

    private val binanceApi = BinanceChainApi(networkType)

    fun getTokenSymbolsAsync(words: List<String>, passphrase: String): Single<List<String>> {
        val address = BinanceChainKit.wallet(words, passphrase, networkType).address

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
