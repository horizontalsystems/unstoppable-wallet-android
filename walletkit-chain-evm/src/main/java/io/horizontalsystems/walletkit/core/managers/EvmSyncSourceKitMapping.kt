package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.providers.IAppConfigProvider
import io.horizontalsystems.walletkit.entities.EvmSyncSource
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType

// Maps the kit-free sync-source entity to ethereum-kit types at the kit boundary.
val EvmSyncSource.rpcSource: RpcSource
    get() = if (isWebSocket) {
        RpcSource.WebSocket(uri, auth)
    } else {
        RpcSource.Http(uris, auth)
    }

fun evmTransactionSource(blockchainType: BlockchainType, appConfigProvider: IAppConfigProvider): TransactionSource =
    when (blockchainType) {
        BlockchainType.Ethereum -> TransactionSource.ethereum(appConfigProvider.etherscanApiKey)
        BlockchainType.BinanceSmartChain -> TransactionSource.binance(appConfigProvider.bscscanApiKey)
        BlockchainType.Avalanche -> TransactionSource.avalanche(appConfigProvider.bscscanApiKey)
        BlockchainType.Optimism -> TransactionSource.optimism(appConfigProvider.bscscanApiKey)
        BlockchainType.Base -> TransactionSource.base(appConfigProvider.bscscanApiKey)
        BlockchainType.Polygon -> TransactionSource.polygon(appConfigProvider.etherscanApiKey)
        BlockchainType.ArbitrumOne -> TransactionSource.arbitrumOne(appConfigProvider.etherscanApiKey)
        BlockchainType.Gnosis -> TransactionSource.gnosis(appConfigProvider.etherscanApiKey)
        BlockchainType.Fantom -> TransactionSource.fantom(appConfigProvider.etherscanApiKey)
        BlockchainType.ZkSync -> TransactionSource.zkSync(appConfigProvider.otherScanApiKey)
        BlockchainType.RobinhoodChain -> TransactionSource.robinhood(appConfigProvider.etherscanApiKey)
        else -> throw IllegalArgumentException("No transaction source for ${blockchainType.uid}")
    }
