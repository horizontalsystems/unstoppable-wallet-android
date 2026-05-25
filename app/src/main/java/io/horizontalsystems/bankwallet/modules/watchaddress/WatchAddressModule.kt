package io.horizontalsystems.bankwallet.modules.watchaddress

import io.horizontalsystems.marketkit.models.BlockchainType

object WatchAddressModule {

    val supportedBlockchainTypes = buildList {
        add(BlockchainType.Ethereum)
        add(BlockchainType.Tron)
        add(BlockchainType.Ton)
        add(BlockchainType.Bitcoin)
        add(BlockchainType.BitcoinCash)
        add(BlockchainType.Litecoin)
        add(BlockchainType.Dash)
        add(BlockchainType.ECash)
        add(BlockchainType.Stellar)
        add(BlockchainType.Monero)
    }
}
