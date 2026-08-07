package io.horizontalsystems.walletkit.modules.address

import com.unstoppabledomains.resolution.Resolution
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.BitcoinAddress
import io.horizontalsystems.walletkit.entities.MoneroWatchAddress
import io.horizontalsystems.walletkit.modules.watchaddress.MoneroUriParser
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
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
        if (!value.contains(".")) return false

        // Refuse to resolve a name whose letters mix scripts: it would be shown to the user as the
        // destination while imitating a different, familiar name.
        if (DomainScriptCheck.isMixedScript(value)) return false

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
        if (!value.contains(".")) return false

        // Same reasoning as the ENS handler: a mixed-script name can imitate a familiar one.
        if (DomainScriptCheck.isMixedScript(value)) return false

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
            BlockchainType.Polygon,
            BlockchainType.Optimism,
            BlockchainType.Base,
            BlockchainType.ZkSync,
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
            BlockchainType.Stellar -> "XLM"
            BlockchainType.Thorchain -> "RUNE"
            BlockchainType.Mayachain -> "CACAO"
            BlockchainType.Monero -> "XMR"
            BlockchainType.Zano -> "ZANO"
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
                BlockchainType.Base,
                BlockchainType.ZkSync,
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





class AddressHandlerPure(override val blockchainType: BlockchainType) : IAddressHandler {

    override fun isSupported(value: String) = true

    override fun parseAddress(value: String) = Address(value, blockchainType = blockchainType)

}