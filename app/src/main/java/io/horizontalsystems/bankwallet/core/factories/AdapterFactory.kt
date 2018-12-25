package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.BitcoinAdapter
import io.horizontalsystems.bankwallet.core.EthereumAdapter
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.entities.CoinType

class AdapterFactory(private val appConfigProvider: IAppConfigProvider) {

    fun adapterForCoin(coinType: CoinType, words: List<String>, newWallet: Boolean, walletId: String?) = when (coinType) {
            is CoinType.Bitcoin -> BitcoinAdapter.createBitcoin(words, appConfigProvider.testMode, newWallet, walletId)
            is CoinType.BitcoinCash -> BitcoinAdapter.createBitcoinCash(words, appConfigProvider.testMode, newWallet, walletId)
            is CoinType.Ethereum -> EthereumAdapter.createEthereum(words, appConfigProvider.testMode)
            is CoinType.Erc20 -> null
    }

}
