package cash.p.terminal.core.factories

import cash.p.terminal.core.utils.AddressParser
import io.horizontalsystems.marketkit.models.BlockchainType

object AddressParserFactory {
    val uriBlockchainTypes: List<BlockchainType> = listOf(
        BlockchainType.Bitcoin,
        BlockchainType.BitcoinCash,
        BlockchainType.ECash,
        BlockchainType.Dash,
        BlockchainType.Litecoin,
        BlockchainType.Zcash,
        BlockchainType.Ethereum,
        BlockchainType.BinanceChain,
        BlockchainType.Tron,
    )

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
        BlockchainType.Tron -> AddressParser("tron", true)
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.ArbitrumOne,
        BlockchainType.Solana,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.Tron,
        BlockchainType.Ton,
        is BlockchainType.Unsupported -> AddressParser("", false)
    }
}
