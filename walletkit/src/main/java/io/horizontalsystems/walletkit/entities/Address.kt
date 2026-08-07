package io.horizontalsystems.walletkit.entities

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.serialization.Serializable

@Serializable
open class Address(
    val hex: String,
    val domain: String? = null,
    val blockchainType: BlockchainType? = null,
) {
    val title: String
        get() = domain ?: hex
}

class MoneroWatchAddress(
    val address: String,
    val viewKey: String,
    val height: Long?
): Address(address, blockchainType = BlockchainType.Monero)

class BitcoinAddress(
    hex: String,
    domain: String?,
    blockchainType: BlockchainType?,
    val derivation: TokenType.Derivation?
) : Address(hex, domain, blockchainType)

val BitcoinAddress.tokenType: TokenType
    get() = when (this.blockchainType) {
        BlockchainType.Bitcoin -> TokenType.Derived(this.derivation ?: TokenType.Derivation.Bip84)
        BlockchainType.BitcoinCash -> TokenType.AddressTyped(TokenType.AddressType.Type145)
        BlockchainType.ECash -> TokenType.Native
        BlockchainType.Litecoin -> TokenType.Derived(this.derivation ?: TokenType.Derivation.Bip84)
        BlockchainType.Dash -> TokenType.Native

        BlockchainType.Zcash,
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.Base,
        BlockchainType.ZkSync,
        BlockchainType.ArbitrumOne,
        BlockchainType.Solana,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.Tron,
        BlockchainType.Ton,
        BlockchainType.Stellar,
        BlockchainType.Thorchain,
        BlockchainType.Mayachain,
        BlockchainType.Monero,
        BlockchainType.Zano,
        is BlockchainType.Unsupported,
        null -> TokenType.Unsupported("", "")
    }

fun Address?.getEthereumKitAddress(): io.horizontalsystems.ethereumkit.models.Address? {
    val hex = this?.hex ?: return null

    return try {
        io.horizontalsystems.ethereumkit.models.Address(hex)
    } catch (err: Exception) {
        null
    }
}
