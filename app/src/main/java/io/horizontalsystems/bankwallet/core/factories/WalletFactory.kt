package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Wallet

class WalletFactory(private val adapterFactory: AdapterFactory) {

    fun createWallet(coin: Coin, words: List<String>): Wallet? {
        val adapter = adapterFactory.adapterForCoin(coin.type, words, false, "walletId") ?: return null
        adapter.start()

        return Wallet(coin.title, coin.code, adapter)
    }

}
