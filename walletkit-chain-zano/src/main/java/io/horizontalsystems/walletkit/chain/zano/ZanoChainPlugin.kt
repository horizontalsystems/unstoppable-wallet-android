package io.horizontalsystems.walletkit.chain.zano

import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.adapters.ZanoAdapter
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.adapters.ZanoSendUnknownException
import io.horizontalsystems.walletkit.core.chain.NodeError
import io.horizontalsystems.walletkit.core.managers.NodeAutoSelector
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.BackgroundManager
import io.horizontalsystems.walletkit.core.managers.ZanoKitManager
import io.horizontalsystems.walletkit.core.managers.ZanoNodeManager
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.chain.SendChainSettings
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import java.math.BigDecimal
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceZano
import io.horizontalsystems.zanokit.ZanoWallet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import io.horizontalsystems.walletkit.core.managers.SourceProbe
import io.horizontalsystems.zanokit.NodePingResult
import io.horizontalsystems.walletkit.core.managers.statusInfo
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.modules.blockchainsettings.BlockchainSettingsModule
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.modules.zanonetwork.ZanoNetworkPage
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.core.chain.SendMemoSupport
import io.horizontalsystems.walletkit.modules.memo.MemoVisibility
import io.horizontalsystems.zanokit.ZanoKit
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.Date

/**
 * Managers are passed as providers so the plugin's pure hooks (metadata, support matrix)
 * stay constructible without Android/runtime dependencies (e.g. in the parity test).
 */
class ZanoChainPlugin(
    private val zanoNodeManager: () -> ZanoNodeManager,
    private val backgroundManager: () -> BackgroundManager,
) : ChainPlugin {

    override fun sendTransactionData(
        token: Token,
        amount: BigDecimal,
        address: String,
        memo: String?,
        extraInput: String?,
        settings: SendChainSettings?,
    ) = SendTransactionData.Zano(address, amount, memo)

    // The memo is sent as the transfer comment, which Zano stores on-chain encrypted so that
    // only the sender and the recipient can read it.
    override suspend fun sendMemoSupport(token: Token, address: String?) =
        SendMemoSupport(maxBytes = 120, visibility = MemoVisibility.Encrypted)

    // Created on first use: before any Zano wallet exists the kit manager's node/background
    // subscriptions are no-ops, so lazy construction preserves startup behavior.
    private val kitManager by lazy { ZanoKitManager(zanoNodeManager(), backgroundManager()) }


    override val blockchainType: BlockchainType = BlockchainType.Zano

    override fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter =
        ZanoAdapter.create(
            wallet = wallet,
            zanoKitManager = kitManager,
            restoreSettings = restoreSettings,
        )

    override fun unlink(account: Account) {
        kitManager.unlink(account)
    }

    override fun addressHandlers(): List<IAddressHandler> = listOf(AddressHandlerZano())

    override fun domainAddressHandlers(): List<IAddressHandler> =
        listOf(AddressHandlerZanoAlias(ZanoAliasResolver(zanoNodeManager())))

    override fun addressValidator(token: Token): EnterAddressValidator = ZanoAddressValidator()

    override fun newWalletBirthdayHeight(): Long = ZanoKit.restoreHeightForDate(Date())

    override fun firstBlockDate(): LocalDate = LocalDate.of(2019, 5, 8)

    override suspend fun estimateBlockDate(height: Long): Date? = ZanoKit.dateForRestoreHeight(height)

    override suspend fun estimateBlockHeightFromDate(date: Date): Long = ZanoKit.restoreHeightForDate(date)

    // Not implemented: zano-kit exposes no clear API, unlike bitcoin-kit and ethereum-kit which
    // provide Kit.clear(context, network, walletId). Deleting the store from here would mean
    // hardcoding the kit's internal database name, which silently stops working the moment the
    // kit renames it. So Zano account data currently outlives the account — history and
    // addresses, not keys. Needs a clear() in zano-kit; see #9452.
    override fun clearAccountData(accountId: String) = Unit

    // Only a restart of the kit on its node: a node change keeps the wallet and its adapter
    override val walletReloadTrigger: Flow<*>
        get() = zanoNodeManager().currentNodeUpdatedFlow

    override val settingsRefreshTrigger: Flow<*>
        get() = zanoNodeManager().nodesUpdatedFlow

    private val reselectScope = CoroutineScope(Dispatchers.Default)

    override suspend fun onAppStart() {
        val nodeManager = zanoNodeManager()
        nodeManager.nodePinger = { nodes ->
            val results = ZanoKit.pingNodes(nodes.map { it.host }).associateBy { it.daemonAddress }
            nodes.map { node -> nodeProbe(node, results[node.host]) }
        }

        // Auto-Select: probe only when Zano is in use. The kit has already started on the
        // stored node; it is restarted on another one only if that node is down or behind.
        if (!isInUseAtStartup()) return
        nodeManager.repickNode()
    }

    override suspend fun onEnterForeground() = reselectNodeIfStalled(retryIfNoSwitch = false)

    override suspend fun refreshKit() = reselectNodeIfStalled(retryIfNoSwitch = false)

    // The watcher's two-strike gate means the stall is persistent, so when no other node
    // answers the kit is restarted on the current one. Foreground and refresh skip that:
    // they can fire on a transient blip where a forced restart would churn.
    override suspend fun onSyncStalled() = reselectNodeIfStalled(retryIfNoSwitch = true, knownStalled = true)

    // The Zano wallet keeps reporting itself synced after its daemon stops answering: it only
    // reports a daemon it never reached. So with Auto-Select on the current node is asked
    // directly, one small request per check, and only while a Zano wallet is in use.
    override suspend fun isSyncStalled(): Boolean = when {
        hasUnsyncedWallet() -> true
        !hasActiveWallet() || !App.connectivityManager.isConnected -> false
        else -> zanoNodeManager().isCurrentNodeDown()
    }

    private fun hasActiveWallet() =
        App.walletManager.activeWallets.any { it.token.blockchainType == BlockchainType.Zano }

    /**
     * Moves to another built-in node when the current one has stopped working.
     *
     * Gated on a stalled sync so a healthy connection is never churned, and on connectivity
     * so a dead phone network is not mistaken for a dead node.
     *
     * @param knownStalled the caller has already established the stall.
     */
    private fun reselectNodeIfStalled(retryIfNoSwitch: Boolean, knownStalled: Boolean = false) {
        if (!App.connectivityManager.isConnected) return

        // Launched rather than awaited: the probes can take seconds, refreshKit() is awaited by
        // the balance pull-to-refresh spinner and the foreground hook runs plugins one after
        // another. The switch lands through walletReloadTrigger, and the manager lets only one
        // reselection run at a time.
        reselectScope.launch {
            try {
                val nodeManager = zanoNodeManager()
                // With the switch off the node is the user's choice and is left alone.
                if (!nodeManager.autoSelectEnabled) return@launch
                // The watcher has just asked; foreground and refresh have to ask themselves.
                if (!knownStalled && !isSyncStalled()) return@launch

                val reselection = nodeManager.reselectNode()
                // Restarting helps a kit that gave up connecting, not one whose node is down.
                // Not after a reselection that was skipped: another one is at work, or the
                // user has taken over, and a restart would cut across that.
                if (reselection == NodeAutoSelector.Reselection.Stayed && retryIfNoSwitch && hasUnsyncedWallet()) {
                    nodeManager.retryCurrentNode()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (expected: Throwable) {
                // Launched on its own: a failure here must not reach the app
                Timber.e(expected, "Zano node reselect failed")
            }
        }
    }

    // The kit deals with a node that fails a send itself: before the broadcast it moves to
    // another node and builds the transfer again, after it it settles the same transaction
    // with the other nodes. What is left for the screen is a transfer no node confirmed.
    override fun classifyNodeError(error: Throwable): NodeError? = when (error) {
        is ZanoSendUnknownException -> NodeError(preBroadcast = false)
        else -> null
    }

    // The node changes with the wallet open, so the adapter stays and there is nothing to wait for
    override suspend fun failoverForSend(token: Token): String? =
        zanoNodeManager().switchAwayFromNode()?.name

    override fun statusInfo(): Map<String, Any>? = kitManager.statusInfo

    override fun networkSettingsPage(): HSPage = ZanoNetworkPage

    override fun blockchainSettingsItem(): BlockchainSettingsModule.BlockchainItem.Chain? {
        val nodeManager = zanoNodeManager()
        val blockchain = nodeManager.blockchain ?: return null
        return BlockchainSettingsModule.BlockchainItem.Chain(
            blockchain = blockchain,
            subtitle = nodeManager.currentNode.name,
            btcLike = true,
            page = ZanoNetworkPage,
            statEvent = StatEvent.OpenBlockchainSettingsCryptoNote(blockchain.uid),
        )
    }

    override suspend fun swapDestinationAddress(account: Account): String? {
        val accountType = account.type as? AccountType.Mnemonic ?: return null
        return withContext(Dispatchers.IO) {
            ZanoKit.address(ZanoWallet.Bip39(accountType.words, accountType.passphrase, 0))
        }
    }

    override fun sendTransactionService(token: Token): AbstractSendTransactionService {
        val adapter = App.adapterManager.getAdapterForToken<ZanoAdapter>(token)
            ?: throw IllegalStateException("ZanoAdapter is null")
        return SendTransactionServiceZano(adapter)
    }

}

/** A kit ping as the Auto-Select probe of [node]: a node that is not valid has neither time nor height. */
internal fun nodeProbe(node: ZanoNodeManager.ZanoNode, ping: NodePingResult?): SourceProbe<ZanoNodeManager.ZanoNode> =
    if (ping != null && ping.isValid && ping.responseTime < Double.MAX_VALUE) {
        SourceProbe(node, ping.responseTime, ping.height)
    } else {
        SourceProbe(node, null, null)
    }
