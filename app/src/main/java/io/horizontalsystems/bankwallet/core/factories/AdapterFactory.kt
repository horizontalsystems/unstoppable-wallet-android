package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.BitcoinAdapter
import io.horizontalsystems.bankwallet.core.EthereumAdapter
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.ethereumkit.EthereumKit

class AdapterFactory {

    fun adapterForCoin(coinCode: CoinCode, words: List<String>, newWallet: Boolean) = when (coinCode) {
        "BTC" -> BitcoinAdapter(words, BitcoinKit.NetworkType.MainNet, newWallet)
        "BTCt" -> BitcoinAdapter(words, BitcoinKit.NetworkType.TestNet, newWallet)
        "BTCr" -> BitcoinAdapter(words, BitcoinKit.NetworkType.RegTest, newWallet)
        "BCH" -> BitcoinAdapter(words, BitcoinKit.NetworkType.MainNetBitCash, newWallet)
        "BCHt" -> BitcoinAdapter(words, BitcoinKit.NetworkType.TestNetBitCash, newWallet)
        "ETH" -> EthereumAdapter(words, EthereumKit.NetworkType.MainNet)
        "ETHt" -> EthereumAdapter(words, EthereumKit.NetworkType.Kovan)
        else -> null
    }

}
