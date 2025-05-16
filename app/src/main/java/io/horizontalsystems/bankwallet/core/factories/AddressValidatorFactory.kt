package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.ISendStellarAdapter
import io.horizontalsystems.bankwallet.core.ISendTronAdapter
import io.horizontalsystems.bankwallet.core.ISendZcashAdapter
import io.horizontalsystems.bankwallet.modules.send.address.BitcoinAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.EnterAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.EvmAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.SolanaAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.StellarAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.TonAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.TronAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.ZcashAddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token

object AddressValidatorFactory {

    fun get(token: Token): EnterAddressValidator {
        return when (token.blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.ECash,
            BlockchainType.Litecoin,
            BlockchainType.Dash -> {
                val sendAdapter =
                    App.adapterManager.getAdapterForToken<ISendBitcoinAdapter>(token)
                        ?: throw IllegalStateException("SendAdapter is null")
                BitcoinAddressValidator(sendAdapter)
            }

            BlockchainType.Zcash -> {
                val sendAdapter =
                    App.adapterManager.getAdapterForToken<ISendZcashAdapter>(token)
                        ?: throw IllegalStateException("SendAdapter is null")
                ZcashAddressValidator(sendAdapter)
            }

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

            BlockchainType.Solana -> {
                SolanaAddressValidator()
            }

            BlockchainType.Tron -> {
                val sendAdapter =
                    App.adapterManager.getAdapterForToken<ISendTronAdapter>(token)
                        ?: throw IllegalStateException("SendAdapter is null")
                TronAddressValidator(sendAdapter, token)
            }

            BlockchainType.Ton -> {
                TonAddressValidator()
            }

            is BlockchainType.Stellar -> {
                val sendAdapter =
                    App.adapterManager.getAdapterForToken<ISendStellarAdapter>(token)
                        ?: throw IllegalStateException("SendAdapter is null")

                StellarAddressValidator(sendAdapter)
            }
            is BlockchainType.Unsupported -> throw IllegalStateException("Unsupported blockchain type: ${token.blockchainType}")
        }
    }

}
