package io.horizontalsystems.bankwallet.modules.address

import com.unstoppabledomains.resolution.Resolution
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAddressValidator
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.BitcoinAddress
import io.horizontalsystems.binancechainkit.helpers.Crypto
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.CashAddressConverter
import io.horizontalsystems.bitcoincore.utils.SegwitAddressConverter
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tonkit.TonKit
import io.horizontalsystems.tronkit.account.AddressHandler
import org.web3j.ens.EnsResolver

interface IAddressHandler {
    val blockchainType: BlockchainType
    fun isSupported(value: String): Boolean
    fun parseAddress(value: String): Address
}

class AddressHandlerEns(
    override val blockchainType: BlockchainType,
    private val ensResolver: EnsResolver
) : IAddressHandler {
    private val cache = mutableMapOf<String, Address>()

    override fun isSupported(value: String): Boolean {
        if (!EnsResolver.isValidEnsName(value)) return false

        try {
            cache[value] = Address(ensResolver.resolve(value), value, blockchainType)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    override fun parseAddress(value: String): Address {
        return cache[value]!!
    }
}

class AddressHandlerUdn(
    private val tokenQuery: TokenQuery,
    private val coinCode: String?,
    apiKey: String
) : IAddressHandler {
    private val resolution = Resolution(apiKey)
    private val chain by lazy { chain(tokenQuery) }
    private val chainCoinCode by lazy { chainCoinCode(tokenQuery.blockchainType) }
    private val cache = mutableMapOf<String, Address>()

    override val blockchainType = tokenQuery.blockchainType

    override fun isSupported(value: String): Boolean {
        return try {
            cache[value] = Address(resolveAddress(value), value, blockchainType)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun parseAddress(value: String): Address {
        return cache[value]!!
    }

    private fun resolveAddress(value: String): String {
        val fetchers = mutableListOf<() -> String?>()
        chain?.let { chain ->
            coinCode?.let { coinCode ->
                fetchers.add {
                    resolution.getMultiChainAddress(value, coinCode, chain)
                }
            }
        }
        coinCode?.let { coinCode ->
            fetchers.add {
                resolution.getAddress(value, coinCode)
            }
        }
        fetchers.add {
            resolution.getAddress(value, chainCoinCode)
        }

        var lastError: Throwable? = null
        for (fetcher in fetchers) {
            try {
                fetcher.invoke()?.let { resolvedAddress ->
                    return resolvedAddress
                }
            } catch (t: Throwable) {
                lastError = t
            }
        }

        throw lastError!!
    }

    companion object {
        private fun chainCoinCode(blockchainType: BlockchainType) = when (blockchainType) {
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.BinanceChain,
            BlockchainType.Polygon,
            BlockchainType.Optimism,
            BlockchainType.Avalanche,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
            BlockchainType.ArbitrumOne -> "ETH"

            BlockchainType.Bitcoin -> "BTC"
            BlockchainType.BitcoinCash -> "BCH"
            BlockchainType.ECash -> "XEC"
            BlockchainType.Litecoin -> "LTC"
            BlockchainType.Dash -> "DASH"
            BlockchainType.Zcash -> "ZEC"
            BlockchainType.Solana -> "SOL"
            BlockchainType.Tron -> "TRX"
            BlockchainType.Ton -> "TON"
            is BlockchainType.Unsupported -> blockchainType.uid
        }

        private fun chain(tokenQuery: TokenQuery) = when (tokenQuery.tokenType) {
            TokenType.Native -> when (tokenQuery.blockchainType) {
                BlockchainType.Polygon -> "MATIC"
                else -> null
            }

            is TokenType.Eip20 -> when (tokenQuery.blockchainType) {
                BlockchainType.BinanceSmartChain -> "BEP20"
                BlockchainType.Polygon -> "MATIC"
                BlockchainType.Avalanche -> "AVAX"
                BlockchainType.Ethereum,
                BlockchainType.Optimism,
                BlockchainType.ArbitrumOne,
                BlockchainType.Gnosis,
                BlockchainType.Fantom -> "ERC20"

                else -> null
            }

            else -> null
        }
    }

}

class AddressHandlerEvm(override val blockchainType: BlockchainType) : IAddressHandler {

    override fun isSupported(value: String) = try {
        AddressValidator.validate(value)
        true
    } catch (e: AddressValidator.AddressValidationException) {
        false
    }

    override fun parseAddress(value: String): Address {
        val evmAddress = io.horizontalsystems.ethereumkit.models.Address(value)
        return Address(evmAddress.hex, blockchainType = blockchainType)
    }

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
        return BitcoinAddress(hex = address.stringValue, domain = null, blockchainType = blockchainType, scriptType = address.scriptType)
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
        return BitcoinAddress(hex = address.stringValue, domain = null, blockchainType = blockchainType, scriptType = address.scriptType)
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
        return BitcoinAddress(hex = address.stringValue, domain = null, blockchainType = blockchainType, scriptType = address.scriptType)
    }
}

class AddressHandlerBinanceChain : IAddressHandler {
    override val blockchainType = BlockchainType.BinanceChain
    override fun isSupported(value: String) = try {
        Crypto.decodeAddress(value)
        true
    } catch (e: Throwable) {
        false
    }

    override fun parseAddress(value: String): Address {
        Crypto.decodeAddress(value)
        return Address(value, blockchainType = blockchainType)
    }
}

class AddressHandlerSolana : IAddressHandler {
    override fun isSupported(value: String): Boolean {
        return try {
            io.horizontalsystems.solanakit.models.Address(value)
            true
        } catch (e: Throwable) {
            false
        }
    }

    override val blockchainType = BlockchainType.Solana

    override fun parseAddress(value: String): Address {
        try {
            //simulate steps in Solana kit init
            io.horizontalsystems.solanakit.models.Address(value)
        } catch (e: Throwable) {
            throw AddressValidator.AddressValidationException(e.message ?: "")
        }

        return Address(value, blockchainType = blockchainType)
    }

}

class AddressHandlerZcash : IAddressHandler {
    override val blockchainType = BlockchainType.Zcash

    override fun isSupported(value: String): Boolean {
        return ZcashAddressValidator.validate(value)
    }

    override fun parseAddress(value: String): Address {
        return Address(value, blockchainType = blockchainType)
    }

}

class AddressHandlerTron : IAddressHandler {
    override val blockchainType = BlockchainType.Tron

    override fun isSupported(value: String) = try {
        io.horizontalsystems.tronkit.models.Address.fromBase58(value)
        true
    } catch (e: AddressHandler.AddressValidationException) {
        false
    } catch (e: IllegalArgumentException) {
        false
    }

    override fun parseAddress(value: String): Address {
        val tronAddress = io.horizontalsystems.tronkit.models.Address.fromBase58(value)
        return Address(tronAddress.base58, blockchainType = blockchainType)
    }
}

class AddressHandlerTon : IAddressHandler {
    override val blockchainType = BlockchainType.Ton

    override fun isSupported(value: String) = try {
        TonKit.validate(value)
        true
    } catch (e: Exception) {
        false
    }

    override fun parseAddress(value: String): Address {
        return Address(value, blockchainType = blockchainType)
    }
}

class AddressHandlerPure(override val blockchainType: BlockchainType) : IAddressHandler {

    override fun isSupported(value: String) = true

    override fun parseAddress(value: String) = Address(value, blockchainType = blockchainType)

}