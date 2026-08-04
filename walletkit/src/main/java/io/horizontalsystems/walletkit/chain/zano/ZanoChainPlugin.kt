package io.horizontalsystems.walletkit.chain.zano

import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.managers.ZanoKitManager
import io.horizontalsystems.walletkit.core.managers.ZanoNodeManager
import io.horizontalsystems.marketkit.models.BlockchainType

class ZanoChainPlugin(
    private val zanoNodeManager: ZanoNodeManager,
    private val zanoKitManager: ZanoKitManager,
) : ChainPlugin {

    override val blockchainType: BlockchainType = BlockchainType.Zano
}
