package io.horizontalsystems.walletkit.chain.zano

import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.managers.ZanoKitManager
import io.horizontalsystems.walletkit.core.managers.ZanoNodeManager
import io.horizontalsystems.marketkit.models.BlockchainType

/**
 * Managers are passed as providers so the plugin's pure hooks (metadata, support matrix)
 * stay constructible without Android/runtime dependencies (e.g. in the parity test).
 */
class ZanoChainPlugin(
    private val zanoNodeManager: () -> ZanoNodeManager,
    private val zanoKitManager: () -> ZanoKitManager,
) : ChainPlugin {

    override val blockchainType: BlockchainType = BlockchainType.Zano
}
