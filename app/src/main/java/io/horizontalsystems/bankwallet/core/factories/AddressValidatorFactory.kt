package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.ISendTronAdapter
import io.horizontalsystems.bankwallet.core.ISendZcashAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.address.BitcoinAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.EnterAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.EvmAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.SolanaAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.TonAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.TronAddressValidator
import io.horizontalsystems.bankwallet.modules.send.address.ZcashAddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType

object AddressValidatorFactory {

    fun get(wallet: Wallet): EnterAddressValidator {
        val adapter = App.adapterManager.getAdapterForWallet(wallet)

        return when (wallet.token.blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.ECash,
            BlockchainType.Litecoin,
            BlockchainType.Dash -> {
                val sendAdapter = (adapter as? ISendBitcoinAdapter) ?: throw IllegalStateException("SendAdapter is null")
                BitcoinAddressValidator(sendAdapter)
            }

            BlockchainType.Zcash -> {
                val sendAdapter = (adapter as? ISendZcashAdapter) ?: throw IllegalStateException("SendAdapter is null")
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
                val sendAdapter = (adapter as? ISendTronAdapter) ?: throw IllegalStateException("SendAdapter is null")
                TronAddressValidator(sendAdapter, wallet.token)
            }

            BlockchainType.Ton -> {
                TonAddressValidator()
            }

            is BlockchainType.Unsupported -> throw IllegalStateException("Unsupported blockchain type: ${wallet.token.blockchainType}")
        }
    }

}
