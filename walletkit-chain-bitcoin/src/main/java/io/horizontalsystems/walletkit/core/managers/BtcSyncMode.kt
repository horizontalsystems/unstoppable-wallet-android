package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.bitcoincore.BitcoinCore.SyncMode
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.BtcRestoreMode
import io.horizontalsystems.marketkit.models.BlockchainType

private val blockchairSyncEnabledBlockchains =
    listOf(BlockchainType.Bitcoin, BlockchainType.BitcoinCash, BlockchainType.Litecoin)

/** Maps the stored restore mode to the kit's sync mode; lives with the kits in the chain module. */
fun BtcBlockchainManager.syncMode(blockchainType: BlockchainType, accountOrigin: AccountOrigin): SyncMode {
    if (accountOrigin == AccountOrigin.Created && blockchainType in blockchairSyncEnabledBlockchains) {
        return SyncMode.Blockchair()
    }

    return when (restoreMode(blockchainType)) {
        BtcRestoreMode.Blockchair -> SyncMode.Blockchair()
        BtcRestoreMode.Hybrid -> SyncMode.Api()
        BtcRestoreMode.Blockchain -> SyncMode.Full()
    }
}
