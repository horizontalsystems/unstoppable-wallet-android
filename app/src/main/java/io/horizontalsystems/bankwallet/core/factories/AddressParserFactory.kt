package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.utils.AddressParser
import io.horizontalsystems.marketkit.models.BlockchainType

class AddressParserFactory {
    fun parser(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.Bitcoin -> AddressParser("bitcoin", true)
        BlockchainType.BitcoinCash -> AddressParser("bitcoincash", false)
        BlockchainType.ECash -> AddressParser("ecash", false)
        BlockchainType.Litecoin -> AddressParser("litecoin", true)
        BlockchainType.Dash -> AddressParser("dash", true)
        BlockchainType.Zcash -> AddressParser("zcash", true)
        BlockchainType.Ethereum -> AddressParser("ethereum", true)
        BlockchainType.BinanceSmartChain -> AddressParser("", true)
        BlockchainType.BinanceChain -> AddressParser("binance", true)
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.ArbitrumOne,
        BlockchainType.Solana,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.Tron,
        is BlockchainType.Unsupported -> AddressParser("", false)
    }
}
