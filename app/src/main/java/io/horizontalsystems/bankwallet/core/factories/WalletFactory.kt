package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Wallet

class WalletFactory(private val adapterFactory: AdapterFactory) {

    fun createWallet(coin: Coin, authData: AuthData): Wallet? {
        val adapter = adapterFactory.adapterForCoin(coin, authData) ?: return null
        adapter.start()

        return Wallet(coin.title, coin.code, adapter)
    }

    fun unlinkWallet(wallet: Wallet) {
        adapterFactory.unlinkAdapter(wallet.adapter)
    }
}
