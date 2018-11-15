package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.BitcoinAdapter
import io.horizontalsystems.bankwallet.core.EthereumAdapter
import io.horizontalsystems.bankwallet.modules.transactions.Coin
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.ethereumkit.EthereumKit

class AdapterFactory {

    fun adapterForCoin(coin: Coin, words: List<String>) = when (coin) {
        "BTC" -> BitcoinAdapter(words, BitcoinKit.NetworkType.MainNet)
        "BTCt" -> BitcoinAdapter(words, BitcoinKit.NetworkType.TestNet)
        "BTCr" -> BitcoinAdapter(words, BitcoinKit.NetworkType.RegTest)
        "BCH" -> BitcoinAdapter(words, BitcoinKit.NetworkType.MainNetBitCash)
        "BCHt" -> BitcoinAdapter(words, BitcoinKit.NetworkType.TestNetBitCash)
        "ETH" -> EthereumAdapter(words, EthereumKit.NetworkType.MainNet)
        "ETHt" -> EthereumAdapter(words, EthereumKit.NetworkType.Kovan)
        else -> null
    }

}
