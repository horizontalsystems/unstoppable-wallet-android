package io.horizontalsystems.walletkit.chain.zano

import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.adapters.ZanoAdapter
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.managers.ZanoKitManager
import io.horizontalsystems.walletkit.core.managers.ZanoNodeManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.Wallet
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

    override fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter =
        ZanoAdapter.create(
            wallet = wallet,
            zanoKitManager = zanoKitManager(),
            restoreSettings = restoreSettings,
        )

    override fun unlink(account: Account) {
        zanoKitManager().unlink(account)
    }
}
