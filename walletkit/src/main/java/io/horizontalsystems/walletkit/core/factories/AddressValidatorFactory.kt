package io.horizontalsystems.walletkit.core.factories

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.EvmAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.TronAddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token

object AddressValidatorFactory {

    // allowOwnAddress: swap recipients may name the user's own address (the default
    // delivery target anyway); the Send flow keeps the send-to-self protection.
    // zcashTransparentOnly: the selected swap route can deliver only to transparent
    // Zcash addresses (CEX providers) — shielded/unified recipients are rejected.
    fun get(
        token: Token,
        allowOwnAddress: Boolean = false,
        zcashTransparentOnly: Boolean = false,
    ): EnterAddressValidator {
        ChainRegistry[token.blockchainType]?.addressValidator(token, allowOwnAddress, zcashTransparentOnly)?.let {
            return it
        }

        return when (token.blockchainType) {
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.Base,
            BlockchainType.ZkSync,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
            BlockchainType.ArbitrumOne -> {
                EvmAddressValidator()
            }


            BlockchainType.Tron -> {
                TronAddressValidator(token, App.adapterManager, allowOwnAddress)
            }

            else -> throw IllegalStateException("Unsupported blockchain type: ${token.blockchainType}")
        }
    }

}
