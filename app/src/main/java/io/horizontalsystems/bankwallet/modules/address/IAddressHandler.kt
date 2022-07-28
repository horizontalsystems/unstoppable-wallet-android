package io.horizontalsystems.bankwallet.modules.address

import com.unstoppabledomains.resolution.Resolution
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import org.kethereum.eip137.model.ENSName
import org.kethereum.ens.ENS
import org.kethereum.ens.isPotentialENSDomain
import org.kethereum.rpc.min3.getMin3RPC

interface IAddressHandler {
    fun isSupported(value: String): Boolean
    fun parseAddress(value: String): Address
}

class AddressHandlerEns : IAddressHandler {
    private val ens = ENS(getMin3RPC())

    private val cache = mutableMapOf<String, Address>()

    override fun isSupported(value: String): Boolean {
        if (!ENSName(value).isPotentialENSDomain()) return false
        val address = ens.getAddress(ENSName(value)) ?: return false

        cache[value] = Address(address.hex, value)
        return true
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

        var lastError: Exception? = null
        for (fetcher in fetchers) {
            try {
                fetcher.invoke()?.let { resolvedAddress ->
                    return resolvedAddress
                }
            } catch (e: Exception) {
                lastError = e
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
                BlockchainType.Ethereum -> "ERC20"
                BlockchainType.BinanceSmartChain -> "BEP20"
                BlockchainType.Polygon -> "MATIC"
                BlockchainType.Avalanche -> "AVAX"
                BlockchainType.Optimism -> "ERC20"
                BlockchainType.ArbitrumOne -> "ERC20"
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

class AddressHandlerPure : IAddressHandler {
    override fun isSupported(value: String) = true

    override fun parseAddress(value: String) = Address(value)

}