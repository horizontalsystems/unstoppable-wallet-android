package io.horizontalsystems.walletkit.core.chain

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.Wallet

/**
 * Per-blockchain extension point. Optional chains implement this and register in
 * [ChainRegistry] at app start; shared code resolves chain-specific behavior through the
 * registry instead of hardcoded `when (blockchainType)` branches, so a consuming app that
 * does not include a chain's module simply has no plugin for it — and no trace of the
 * chain in the UI.
 *
 * Hooks are added incrementally as dispatch points are converted; see
 * docs/Walletkit-Modularization-Plan.md.
 */
interface ChainPlugin {
    val blockchainType: BlockchainType

    /**
     * Creates the balance/send adapter for a wallet of this chain. [restoreSettings] holds
     * the chain's stored restore configuration (e.g. birthday height) and is empty for
     * chains without restore settings. Returning null defers creation (the adapter is
     * recreated on the next reloadWallets for this chain).
     */
    fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter? = null

    /** Releases kit-manager resources held for [account] when its last wallet is unlinked. */
    fun unlink(account: Account) = Unit
}
