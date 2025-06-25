package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.managers.BaseSpamManager
import io.horizontalsystems.bankwallet.core.managers.EvmSpamManager
import io.horizontalsystems.bankwallet.core.managers.TronSpamManager
import io.horizontalsystems.marketkit.models.BlockchainType

class SpamManagerFactory(
    private val evmSpamManager: EvmSpamManager,
    private val tronSpamManager: TronSpamManager,
) {

    fun spamManager(blockchainType: BlockchainType): BaseSpamManager? =
        when (blockchainType) {
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
                evmSpamManager
            }

            BlockchainType.Tron -> {
                tronSpamManager
            }

            else -> null
        }

}
