package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.BitcoinAdapter
import io.horizontalsystems.bankwallet.core.EthereumAdapter
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.ethereumkit.EthereumKit

class AdapterFactory(private val appConfigProvider: IAppConfigProvider) {

    fun adapterForCoin(coin: Coin, words: List<String>, newWallet: Boolean): IAdapter? {
        return when (coin.blockChain) {
            is BlockChain.Bitcoin ->
                when (coin.blockChain.type) {
                    is BitcoinType.Bitcoin -> {
                        val network = when (appConfigProvider.network) {
                            Network.MAIN -> BitcoinKit.NetworkType.MainNet
                            Network.TEST -> BitcoinKit.NetworkType.TestNet
                        }
                        BitcoinAdapter(words, network, newWallet)
                    }
                    is BitcoinType.BitcoinCash -> {
                        val network = when (appConfigProvider.network) {
                            Network.MAIN -> BitcoinKit.NetworkType.MainNetBitCash
                            Network.TEST -> BitcoinKit.NetworkType.TestNetBitCash
                        }
                        BitcoinAdapter(words, network, newWallet)
                    }
                }
            is BlockChain.Ethereum ->
                when (coin.blockChain.type) {
                    is EthereumType.Ethereum -> {
                        val network = when (appConfigProvider.network) {
                            Network.MAIN -> EthereumKit.NetworkType.MainNet
                            Network.TEST -> EthereumKit.NetworkType.Kovan
                        }
                        EthereumAdapter(words, network)
                    }
                    is EthereumType.Erc20 -> {
                        null
                    }
                }
        }
    }

}
