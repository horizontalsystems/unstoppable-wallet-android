package io.horizontalsystems.walletkit.chain.zcash

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.chain.ChainSendScreenArgs
import io.horizontalsystems.walletkit.core.managers.NoActiveAccount
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.managers.ZcashBirthdayProvider
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.blockchainsettings.BlockchainSettingsModule
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceZcash
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.receive.ZcashAddressTypeSelectPage
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.modules.send.zcash.SendZCashModule
import io.horizontalsystems.walletkit.modules.send.zcash.SendZCashScreen
import io.horizontalsystems.walletkit.modules.send.zcash.SendZCashViewModel
import io.horizontalsystems.walletkit.modules.send.zcash.shield.ShieldZcashPage
import io.horizontalsystems.walletkit.modules.zcashmigration.ZcashMigrationPage
import io.horizontalsystems.walletkit.modules.zcashnetwork.ZcashNetworkPage
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Date
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class ZcashChainPlugin(
    private val context: () -> Context,
    private val endpointManager: () -> ZcashLightWalletEndpointManager,
    private val localStorage: () -> ILocalStorage,
) : ChainPlugin {

    private val reselectScope = CoroutineScope(Dispatchers.Default)

    override val blockchainType: BlockchainType = BlockchainType.Zcash

    private val birthdayProvider by lazy { ZcashBirthdayProvider(context()) }

    // Double-checked caches for derived addresses: each derivation spins up a fresh
    // synchronizer, so results are memoized per account (moved from SwapHelper).
    private val transparentAddressCache = ConcurrentHashMap<String, String>()
    private val transparentAddressMutex = Mutex()
    private val unifiedAddressCache = ConcurrentHashMap<String, String>()
    private val unifiedAddressMutex = Mutex()

    override fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter? {
        val manager = endpointManager()
        if (manager.isResolvingFastestEndpoint) {
            // Defer creation until startup Auto-Select picks the fastest endpoint, so the
            // adapter connects once to it instead of reconnecting. reloadWallets(Zcash)
            // recreates the adapter when resolution finishes.
            return null
        }
        return ZcashAdapter(
            context(),
            wallet,
            restoreSettings,
            localStorage(),
            manager.currentEndpoint.toLightWalletEndpoint(),
        )
    }

    override suspend fun onAppStart() {
        val manager = endpointManager()
        manager.endpointPinger = { urls ->
            ZcashEndpointPinger.ping(context(), urls) { url ->
                ZcashLightWalletEndpointManager.ZcashEndpoint(url, url).toLightWalletEndpoint()
            }
        }
        manager.autoSelectFastestEndpointOnStartup()
    }

    override suspend fun onEnterForeground() = reselectFastestIfUnsynced()

    override suspend fun refreshKit() = reselectFastestIfUnsynced()

    override suspend fun onSyncStalled() = reselectFastestIfUnsynced()

    /**
     * Switches to the fastest reachable endpoint when the current one has stopped working.
     *
     * Gated on an unsynced wallet so a healthy connection is never churned, and on connectivity
     * so a dead phone network is not mistaken for a dead server.
     */
    private fun reselectFastestIfUnsynced() {
        if (!App.connectivityManager.isConnected) return
        if (!hasUnsyncedWallet()) return

        // Launched rather than awaited: the probe can take seconds and both callers are latency
        // sensitive — refreshKit() is awaited by the balance pull-to-refresh spinner, and the
        // foreground hook runs plugins one after another. The switch lands asynchronously through
        // walletReloadTrigger. The manager guards with a mutex, so overlapping calls collapse.
        reselectScope.launch {
            try {
                endpointManager().reselectFastestEndpoint()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Timber.e(e, "Zcash endpoint reselect failed")
            }
        }
    }

    override fun clearAccountData(accountId: String) {
        ZcashAdapter.clear(accountId)
    }

    override suspend fun prepareRescan(wallet: Wallet) {
        val adapter: ZcashAdapter? = App.adapterManager.getAdapterForWallet(wallet)
        withContext(Dispatchers.IO) {
            adapter?.stop()
            ZcashAdapter.clear(wallet.account.id)
        }
    }

    override fun addressHandlers(): List<IAddressHandler> = listOf(AddressHandlerZcash())

    override fun addressValidator(token: Token): EnterAddressValidator =
        ZcashAddressValidator(token, App.adapterManager)

    override fun addressValidator(
        token: Token,
        allowOwnAddress: Boolean,
        transparentOnly: Boolean,
    ): EnterAddressValidator = ZcashAddressValidator(token, App.adapterManager, allowOwnAddress, transparentOnly)

    override fun newWalletBirthdayHeight(): Long = birthdayProvider.getLatestCheckpointBlockHeight()

    override fun firstBlockDate(): LocalDate = LocalDate.of(2018, 10, 29)

    override fun minBirthdayHeight(): Long = 420_000L

    override suspend fun estimateBlockDate(height: Long): Date? =
        ZcashAdapter.estimateBirthdayDate(context(), height)

    override suspend fun estimateBlockHeightFromDate(date: Date): Long? =
        ZcashAdapter.estimateBirthdayHeight(context(), date)

    override val walletReloadTrigger: Flow<*>
        get() = endpointManager().currentEndpointUpdatedFlow

    override fun statusInfo(): Map<String, Any>? {
        val wallet = App.walletManager.activeWallets
            .firstOrNull { it.token.blockchainType == BlockchainType.Zcash } ?: return null
        return App.adapterManager.getAdapterForWallet<ZcashAdapter>(wallet)?.statusInfo
    }

    override fun networkSettingsPage(): HSPage = ZcashNetworkPage

    override fun blockchainSettingsItem(): BlockchainSettingsModule.BlockchainItem.Chain? {
        val manager = endpointManager()
        val blockchain = manager.blockchain ?: return null
        return BlockchainSettingsModule.BlockchainItem.Chain(
            blockchain = blockchain,
            subtitle = manager.currentEndpoint.name,
            btcLike = true,
            page = ZcashNetworkPage,
            statEvent = StatEvent.OpenBlockchainSettingsCryptoNote(blockchain.uid),
        )
    }

    override fun receivePage(wallet: Wallet, entryPoint: KClass<out HSPage>?): HSPage =
        ZcashAddressTypeSelectPage(ZcashAddressTypeSelectPage.Input(wallet, entryPoint))

    override fun shieldPage(wallet: Wallet, entryPoint: KClass<out HSPage>): HSPage =
        ShieldZcashPage(ShieldZcashPage.Input(wallet, entryPoint))

    override fun unshieldedBalanceThreshold(): BigDecimal = ZcashAdapter.minimalShieldThreshold

    override fun migrationRequiredBalance(wallet: Wallet): BigDecimal? =
        App.adapterManager.getAdapterForWallet<ZcashAdapter>(wallet)?.ironwoodMigrationRequiredBalance

    override fun migrationPage(wallet: Wallet, entryPoint: KClass<out HSPage>): HSPage =
        ZcashMigrationPage(ZcashMigrationPage.Input(wallet, entryPoint))

    override suspend fun swapDestinationAddress(account: Account): String =
        cachedAddress(account.id, transparentAddressCache, transparentAddressMutex) {
            ZcashAdapter.getTransparentAddress(account, endpointManager().currentEndpoint.toLightWalletEndpoint())
        }

    override suspend fun swapUnifiedReceiveAddress(token: Token): String {
        App.adapterManager.getAdapterForToken<ZcashAdapter>(token)?.let {
            return it.receiveAddress
        }

        val account = App.accountManager.activeAccount ?: throw NoActiveAccount()

        return cachedAddress(account.id, unifiedAddressCache, unifiedAddressMutex) {
            ZcashAdapter.getUnifiedAddress(account, endpointManager().currentEndpoint.toLightWalletEndpoint())
        }
    }

    override fun sendTransactionService(token: Token): AbstractSendTransactionService {
        val adapter = App.adapterManager.getAdapterForToken<ZcashAdapter>(token)
            ?: throw IllegalStateException("ZcashAdapter is null")
        return SendTransactionServiceZcash(adapter)
    }

    @Composable
    override fun SendScreen(args: ChainSendScreenArgs) {
        val factory = SendZCashModule.Factory(args.wallet, args.address, args.hideAddress)
        val sendZCashViewModel = viewModel<SendZCashViewModel>(factory = factory)
        SendZCashScreen(
            title = args.title,
            navigation = args.navigation,
            viewModel = sendZCashViewModel,
            amountInputModeViewModel = args.amountInputModeViewModel,
            sendEntryPointDestId = args.sendEntryPointDestId,
            amount = args.amount,
            riskyAddress = args.riskyAddress,
        )
    }

    // Matches the original SwapHelper semantics: the derivation is memoized per account,
    // serialized by a mutex, and runs NonCancellable so a cancelled caller cannot abandon
    // a half-built synchronizer.
    private suspend fun cachedAddress(
        accountId: String,
        cache: ConcurrentHashMap<String, String>,
        mutex: Mutex,
        derive: suspend () -> String,
    ): String {
        return cache[accountId] ?: mutex.withLock {
            cache[accountId] ?: withContext(NonCancellable + Dispatchers.IO) { derive() }.also { cache[accountId] = it }
        }
    }
}

internal fun ZcashLightWalletEndpointManager.ZcashEndpoint.toLightWalletEndpoint(): LightWalletEndpoint {
    val uri = URI(url)
    return LightWalletEndpoint(
        host = uri.host ?: url,
        port = uri.port.takeIf { it > 0 } ?: 443,
        isSecure = uri.scheme?.lowercase() == "https"
    )
}
