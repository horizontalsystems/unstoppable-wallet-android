package io.horizontalsystems.walletkit.modules.address

import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.dashkit.MainNetDash
import io.horizontalsystems.ecash.MainNetECash
import io.horizontalsystems.litecoinkit.MainNetLitecoin
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.thorchainkit.network.Network as ThorchainNetwork
import io.horizontalsystems.walletkit.core.chain.ChainRegistry

/**
 * Plain (non-domain) address handlers per blockchain — the single source of truth used by
 * both [AddressHandlerFactory] and [AddressInputModule].
 */
fun plainAddressHandlers(blockchainType: BlockchainType): List<IAddressHandler> =
    when (blockchainType) {
        BlockchainType.Bitcoin -> {
            val network = MainNet()
            listOf(
                AddressHandlerBase58(network, blockchainType),
                AddressHandlerBech32(network, blockchainType),
            )
        }

        BlockchainType.BitcoinCash -> {
            val network = MainNetBitcoinCash()
            listOf(
                AddressHandlerBase58(network, blockchainType),
                AddressHandlerBitcoinCash(network, blockchainType),
            )
        }

        BlockchainType.ECash -> {
            val network = MainNetECash()
            listOf(
                AddressHandlerBase58(network, blockchainType),
                AddressHandlerBitcoinCash(network, blockchainType),
            )
        }

        BlockchainType.Litecoin -> {
            val network = MainNetLitecoin()
            listOf(
                AddressHandlerBase58(network, blockchainType),
                AddressHandlerBech32(network, blockchainType),
            )
        }

        BlockchainType.Dash -> {
            val network = MainNetDash()
            listOf(AddressHandlerBase58(network, blockchainType))
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
        BlockchainType.ArbitrumOne -> listOf(AddressHandlerEvm(blockchainType))

        BlockchainType.Tron -> listOf(AddressHandlerTron())
        BlockchainType.Ton -> listOf(AddressHandlerTon())
        BlockchainType.Stellar -> listOf(AddressHandlerStellar())
        BlockchainType.Thorchain -> listOf(AddressHandlerThorchain(ThorchainNetwork.Mainnet, BlockchainType.Thorchain))
        BlockchainType.Mayachain -> listOf(AddressHandlerThorchain(ThorchainNetwork.MayaMainnet, BlockchainType.Mayachain))

        else -> ChainRegistry[blockchainType]?.addressHandlers().orEmpty()
    }
