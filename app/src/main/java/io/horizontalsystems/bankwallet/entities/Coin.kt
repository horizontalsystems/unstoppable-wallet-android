package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

sealed class BitcoinType {
    object Bitcoin : BitcoinType()
    object BitcoinCash : BitcoinType()
}

sealed class EthereumType {
    object Ethereum : EthereumType()
    class Erc20(address: String, decimal: Int) : EthereumType()
}

sealed class BlockChain {
    class Bitcoin(type: BitcoinType) : BlockChain()
    class Ethereum(type: EthereumType) : BlockChain()
}

class Coin(val title: String, val coinCode: CoinCode, val blockChain: BlockChain)
