package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.BitcoinAdapter
import io.horizontalsystems.bankwallet.core.EthereumAdapter
import io.horizontalsystems.bankwallet.modules.transactions.Coin
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.ethereumkit.EthereumKit

class AdapterFactory {

    fun adapterForCoin(coin: Coin, words: List<String>, newWallet: Boolean, walletId: String?) = when (coin) {
        "BTC" -> BitcoinAdapter(words, BitcoinKit.NetworkType.MainNet, newWallet, walletId)
        "BTCt" -> BitcoinAdapter(words, BitcoinKit.NetworkType.TestNet, newWallet, walletId)
        "BTCr" -> BitcoinAdapter(words, BitcoinKit.NetworkType.RegTest, newWallet, walletId)
        "BCH" -> BitcoinAdapter(words, BitcoinKit.NetworkType.MainNetBitCash, newWallet, walletId)
        "BCHt" -> BitcoinAdapter(words, BitcoinKit.NetworkType.TestNetBitCash, newWallet, walletId)
        "ETH" -> EthereumAdapter(words, EthereumKit.NetworkType.MainNet)
        "ETHt" -> EthereumAdapter(words, EthereumKit.NetworkType.Kovan)
        else -> null
    }

}
