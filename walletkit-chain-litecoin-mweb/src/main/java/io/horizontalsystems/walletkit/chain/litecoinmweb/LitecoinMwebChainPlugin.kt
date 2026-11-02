package io.horizontalsystems.walletkit.chain.litecoinmweb

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.ISendMwebAdapter
import io.horizontalsystems.walletkit.core.ITorManager
import io.horizontalsystems.walletkit.core.adapters.LitecoinMwebAdapter
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.chain.SendChainSettings
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.address.AddressHandlerMweb
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceMweb
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.modules.settings.privacy.tor.TorStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import java.math.BigDecimal

/**
 * Litecoin MWEB: the coin LTC held in Litecoin's MimbleWimble extension block. One native
 * token, mnemonic accounts only (finding outputs needs the scan secret), no swap, no memo.
 * Moving coins in from a Litecoin wallet (peg-in) is a send of that wallet, see
 * [io.horizontalsystems.walletkit.core.IPegInMwebAdapter]; moving them out (peg-out) is a
 * send of this wallet to a Litecoin address.
 *
 * The Tor manager is passed as a provider so the pure hooks stay constructible without the
 * runtime (the parity test).
 */
class LitecoinMwebChainPlugin(
    private val torManager: () -> ITorManager,
) : ChainPlugin {

    override val blockchainType: BlockchainType = BlockchainType.LitecoinMweb

    override fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter =
        LitecoinMwebAdapter.create(wallet, torManager())

    override fun addressHandlers(): List<IAddressHandler> = listOf(AddressHandlerMweb(blockchainType))

    override fun addressValidator(token: Token): EnterAddressValidator =
        LitecoinMwebAddressValidator(token, App.adapterManager)

    override fun clearAccountData(accountId: String) {
        LitecoinMwebAdapter.clear(accountId)
    }

    override fun sendTransactionData(
        token: Token,
        amount: BigDecimal,
        address: String,
        memo: String?,
        extraInput: String?,
        settings: SendChainSettings?,
    ) = SendTransactionData.Mweb(address, amount)

    override fun sendTransactionService(token: Token): AbstractSendTransactionService {
        val adapter = App.adapterManager.getAdapterForToken<ISendMwebAdapter>(token)
            ?: throw IllegalStateException("Litecoin MWEB adapter is null")
        return SendTransactionServiceMweb(adapter, token)
    }

    // The daemon reads the Tor SOCKS port once, when it starts, and Tor listens on a new port
    // after every restart, so the wallets are rebuilt whenever Tor connects.
    override val walletReloadTrigger: Flow<*>
        get() = torManager().torStatusFlow.filter { it == TorStatus.Connected }

    override fun statusInfo(): Map<String, Any>? = activeAdapters().firstOrNull()?.statusInfo

    override suspend fun refreshKit() {
        activeAdapters().forEach { it.refresh() }
    }

    override suspend fun onSyncStalled() {
        activeAdapters().forEach { it.refresh() }
    }

    private fun activeAdapters(): List<LitecoinMwebAdapter> =
        App.walletManager.activeWallets
            .filter { it.token.blockchainType == blockchainType }
            .mapNotNull { App.adapterManager.getAdapterForWallet<LitecoinMwebAdapter>(it) }
}
