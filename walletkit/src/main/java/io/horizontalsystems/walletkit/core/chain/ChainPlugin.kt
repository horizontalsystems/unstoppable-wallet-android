package io.horizontalsystems.walletkit.core.chain

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator

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

    /** Handlers recognizing this chain's plain address formats. */
    fun addressHandlers(): List<IAddressHandler> = emptyList()

    /** Handlers resolving this chain's naming/alias systems to addresses. */
    fun domainAddressHandlers(): List<IAddressHandler> = emptyList()

    /** Validator used by send/enter-address flows, or null to fall back to core dispatch. */
    fun addressValidator(token: Token): EnterAddressValidator? = null
}
