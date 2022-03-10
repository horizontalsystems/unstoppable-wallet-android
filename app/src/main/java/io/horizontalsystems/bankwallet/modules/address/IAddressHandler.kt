package io.horizontalsystems.bankwallet.modules.address

import com.unstoppabledomains.resolution.Resolution
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.CoinType

interface IAddressHandler {
    fun isSupported(value: String): Boolean
    fun parseAddress(value: String): Address
}

class AddressHandlerUdn(private val coinType: CoinType, private val coinCode: String) : IAddressHandler {
    private val resolution = Resolution()
    private val chain by lazy { chain(coinType) }
    private val chainCoinCode by lazy { chainCoinCode(coinType) }

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
        private fun chainCoinCode(coinType: CoinType) = when (coinType) {
            is CoinType.Ethereum,
            is CoinType.Erc20,
            is CoinType.BinanceSmartChain,
            is CoinType.Bep20,
            is CoinType.Polygon,
            is CoinType.Mrc20 -> "ETH"
//            is CoinType.EthereumOptimism -> "ETH"
//            is CoinType.OptimismErc20 -> "ETH"
//            is CoinType.EthereumArbitrumOne -> "ETH"
//            is CoinType.ArbitrumOneErc20 -> "ETH"
            else -> null
        }

        private fun chain(coinType: CoinType) = when (coinType) {
            is CoinType.Erc20 -> "ERC20"
            is CoinType.Bep20 -> "BEP20"
            is CoinType.Polygon,
            is CoinType.Mrc20 -> "MATIC"
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