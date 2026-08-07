package io.horizontalsystems.walletkit.modules.address

import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.BitcoinAddress
import io.horizontalsystems.bitcoincore.core.purpose
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.CashAddressConverter
import io.horizontalsystems.bitcoincore.utils.SegwitAddressConverter
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType

internal val ScriptType.derivation: TokenType.Derivation
    get() = when (this.purpose!!) {
        HDWallet.Purpose.BIP44 -> TokenType.Derivation.Bip44
        HDWallet.Purpose.BIP49 -> TokenType.Derivation.Bip49
        HDWallet.Purpose.BIP84 -> TokenType.Derivation.Bip84
        HDWallet.Purpose.BIP86 -> TokenType.Derivation.Bip86
    }

class AddressHandlerBase58(network: Network, override val blockchainType: BlockchainType) : IAddressHandler {
    private val converter = Base58AddressConverter(network.addressVersion, network.addressScriptVersion)

    override fun isSupported(value: String) = try {
        converter.convert(value)
        true
    } catch (e: Throwable) {
        false
    }

    override fun parseAddress(value: String): Address {
        val address = converter.convert(value)
        return BitcoinAddress(hex = address.stringValue, domain = null, blockchainType = blockchainType, derivation = address.scriptType.derivation)
    }
}

class AddressHandlerBech32(network: Network, override val blockchainType: BlockchainType) : IAddressHandler {
    private val converter = SegwitAddressConverter(network.addressSegwitHrp)

    override fun isSupported(value: String) = try {
        converter.convert(value)
        true
    } catch (e: Throwable) {
        false
    }

    override fun parseAddress(value: String): Address {
        val address = converter.convert(value)
        return BitcoinAddress(hex = address.stringValue, domain = null, blockchainType = blockchainType, derivation = address.scriptType.derivation)
    }
}

class AddressHandlerBitcoinCash(network: Network, override val blockchainType: BlockchainType) : IAddressHandler {
    private val converter = CashAddressConverter(network.addressSegwitHrp)

    override fun isSupported(value: String) = try {
        converter.convert(value)
        true
    } catch (e: Throwable) {
        false
    }

    override fun parseAddress(value: String): Address {
        val address = converter.convert(value)
        return BitcoinAddress(hex = address.stringValue, domain = null, blockchainType = blockchainType, derivation = address.scriptType.derivation)
    }
}
