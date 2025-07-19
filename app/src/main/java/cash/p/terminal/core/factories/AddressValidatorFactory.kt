package cash.p.terminal.core.factories

import cash.p.terminal.core.App
import cash.p.terminal.modules.send.address.BitcoinAddressValidator
import cash.p.terminal.modules.send.address.EnterAddressValidator
import cash.p.terminal.modules.send.address.EvmAddressValidator
import cash.p.terminal.modules.send.address.MoneroAddressValidator
import cash.p.terminal.modules.send.address.SolanaAddressValidator
import cash.p.terminal.modules.send.address.StellarAddressValidator
import cash.p.terminal.modules.send.address.TonAddressValidator
import cash.p.terminal.modules.send.address.TronAddressValidator
import cash.p.terminal.modules.send.address.ZcashAddressValidator
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.entities.BlockchainType

object AddressValidatorFactory {

    fun get(token: Token): EnterAddressValidator {
        return when (token.blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.ECash,
            BlockchainType.Litecoin,
            BlockchainType.Cosanta,
            BlockchainType.Dogecoin,
            BlockchainType.PirateCash,
            BlockchainType.Dash -> {
                BitcoinAddressValidator(token, App.adapterManager)
            }

            BlockchainType.Zcash -> {
                ZcashAddressValidator(token, App.adapterManager)
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
                TronAddressValidator(token, App.adapterManager)
            }

            BlockchainType.Ton -> {
                TonAddressValidator()
            }

            is BlockchainType.Stellar -> {
                StellarAddressValidator(token)
            }

            BlockchainType.Monero -> MoneroAddressValidator()

            is BlockchainType.Unsupported -> throw IllegalStateException("Unsupported blockchain type: ${token.blockchainType}")

        }
    }

}
