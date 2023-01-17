package io.horizontalsystems.bankwallet.modules.address

import com.unstoppabledomains.resolution.Resolution
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import org.web3j.ens.EnsResolver

interface IAddressHandler {
    fun isSupported(value: String): Boolean
    fun parseAddress(value: String): Address
}

class AddressHandlerEns(private val ensResolver: EnsResolver) : IAddressHandler {
    private val cache = mutableMapOf<String, Address>()

    override fun isSupported(value: String): Boolean {
        if (!EnsResolver.isValidEnsName(value)) return false

        try {
            cache[value] = Address(ensResolver.resolve(value), value)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    override fun parseAddress(value: String): Address {
        return cache[value]!!
    }
}

class AddressHandlerUdn(private val tokenQuery: TokenQuery, private val coinCode: String) : IAddressHandler {
    private val resolution = Resolution()
    private val chain by lazy { chain(tokenQuery) }
    private val chainCoinCode by lazy { chainCoinCode(tokenQuery.blockchainType) }

    override fun isSupported(value: String): Boolean {
        return value.contains(".") && resolution.isSupported(value)
    }

    override fun parseAddress(value: String): Address {
        return Address(resolveAddress(value), value)
    }

    private fun resolveAddress(value: String): String {
        val fetchers = mutableListOf<() -> String?>()
        fetchers.add {
            chain?.let { resolution.getMultiChainAddress(value, coinCode, it) }
        }
        fetchers.add {
            resolution.getAddress(value, coinCode)
        }
        fetchers.add {
            chainCoinCode?.let { resolution.getAddress(value, it) }
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
            BlockchainType.Avalanche,
            BlockchainType.ArbitrumOne -> "ETH"
            else -> null
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
                BlockchainType.Gnosis -> "ERC20"
                else -> null
            }
            else -> null
        }
    }

}

class AddressHandlerEvm : IAddressHandler {
    override fun isSupported(value: String) = try {
        AddressValidator.validate(value)
        true
    } catch (e: AddressValidator.AddressValidationException) {
        false
    }

    override fun parseAddress(value: String): Address {
        val evmAddress = io.horizontalsystems.ethereumkit.models.Address(value)
        return Address(evmAddress.hex)
    }

}

class AddressHandlerSolana : IAddressHandler {
    override fun isSupported(value: String) = true

    override fun parseAddress(value: String): Address {
        try {
            //simulate steps in Solana kit init
            io.horizontalsystems.solanakit.models.Address(value)
        } catch (e: Throwable) {
            throw AddressValidator.AddressValidationException(e.message ?: "")
        }

        return Address(value)
    }

}

class AddressHandlerPure : IAddressHandler {
    override fun isSupported(value: String) = true

    override fun parseAddress(value: String) = Address(value)

}