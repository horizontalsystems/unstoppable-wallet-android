package io.horizontalsystems.walletkit.chain.thorchain

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.adapters.ThorchainAdapter
import io.horizontalsystems.walletkit.core.adapters.ThorchainTransactionConverter
import io.horizontalsystems.walletkit.core.adapters.ThorchainTransactionsAdapter
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.chain.ChainSendScreenArgs
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.managers.ThorchainAccountManager
import io.horizontalsystems.walletkit.core.managers.ThorchainKitManager
import io.horizontalsystems.walletkit.core.managers.ThorchainRpcSourceManager
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.address.AddressHandlerThorchain
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.blockchainsettings.BlockchainSettingsModule
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.ThorChainProvider
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceThorchain
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.modules.send.address.ThorchainAddressValidator
import io.horizontalsystems.walletkit.modules.send.thorchain.SendThorchainModule
import io.horizontalsystems.walletkit.modules.send.thorchain.SendThorchainScreen
import io.horizontalsystems.walletkit.modules.send.thorchain.SendThorchainViewModel
import io.horizontalsystems.walletkit.modules.thorchainnetwork.ThorchainNetworkPage
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.thorchainkit.network.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ThorchainChainPlugin : ChainPlugin {

    override val blockchainType: BlockchainType = BlockchainType.Thorchain

    val rpcSourceManager by lazy {
        ThorchainRpcSourceManager(
            App.blockchainSettingsStorage,
            App.marketKit,
            BlockchainType.Thorchain,
            ThorchainRpcSourceManager.thorchainSources,
        )
    }

    val kitManager by lazy {
        ThorchainKitManager(App.backgroundManager, rpcSourceManager, Network.Mainnet)
    }

    private val accountManager by lazy {
        ThorchainAccountManager(
            App.accountManager,
            App.walletManager,
            kitManager,
            App.marketKit,
            App.tokenAutoEnableManager,
            BlockchainType.Thorchain,
        )
    }

    override suspend fun onAppStart() {
        accountManager.start()
    }

    override fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter? =
        when (wallet.token.type) {
            TokenType.Native,
            is TokenType.ThorchainAsset -> ThorchainAdapter(kitManager.getThorchainKitWrapper(wallet.account), wallet)
            else -> null
        }

    // Thorchain keeps no per-account store of its own — balances and history come from the API on
    // demand — so there is nothing to delete when an account is removed.
    override fun clearAccountData(accountId: String) = Unit

    override fun createTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val baseToken = App.coinManager.getToken(TokenQuery(BlockchainType.Thorchain, TokenType.Native)) ?: return null
        val thorchainKitWrapper = kitManager.getThorchainKitWrapper(source.account)

        val transactionConverter = ThorchainTransactionConverter(
            App.coinManager,
            source,
            thorchainKitWrapper.thorchainKit.receiveAddress,
            baseToken,
            BlockchainType.Thorchain,
            thorchainKitWrapper.thorchainKit.network,
        )

        return ThorchainTransactionsAdapter(thorchainKitWrapper, transactionConverter)
    }

    override fun unlink(account: Account) {
        kitManager.unlink(account)
    }

    override val walletReloadTrigger: Flow<*>
        get() = kitManager.kitStoppedFlow

    // The settings row shows the current RPC source, so it refreshes on source changes
    // (the kit-stopped signal only fires when a running kit restarts).
    override val settingsRefreshTrigger: Flow<*>
        get() = rpcSourceManager.rpcSourceUpdatedFlow

    override fun statusInfo(): Map<String, Any>? = kitManager.statusInfo

    override suspend fun refreshKit() {
        kitManager.thorchainKitWrapper?.thorchainKit?.refresh()
    }

    override fun networkSettingsPage(): HSPage = ThorchainNetworkPage

    override fun blockchainSettingsItem(): BlockchainSettingsModule.BlockchainItem.Chain? {
        val blockchain = rpcSourceManager.blockchain ?: return null
        return BlockchainSettingsModule.BlockchainItem.Chain(
            blockchain = blockchain,
            subtitle = rpcSourceManager.rpcSource.name,
            btcLike = false,
            page = ThorchainNetworkPage,
            statEvent = StatEvent.Open(StatPage.BlockchainSettingsThorchain),
        )
    }

    override fun backupSyncSourceName(): String = rpcSourceManager.rpcSource.name

    override suspend fun swapDestinationAddress(account: Account): String =
        withContext(Dispatchers.IO) { kitManager.getAddress(account.type) }

    override fun sendTransactionService(token: Token): AbstractSendTransactionService {
        val adapter = App.adapterManager.getAdapterForToken<ThorchainAdapter>(token)
            ?: throw IllegalStateException("ThorchainAdapter is null")
        return SendTransactionServiceThorchain(adapter, BlockchainType.Thorchain)
    }

    override fun swapProviders(): List<IMultiSwapProvider> = listOf(ThorChainProvider)

    override fun addressHandlers(): List<IAddressHandler> =
        listOf(AddressHandlerThorchain(Network.Mainnet, BlockchainType.Thorchain))

    override fun addressValidator(token: Token): EnterAddressValidator = ThorchainAddressValidator(token)

    @Composable
    override fun SendScreen(args: ChainSendScreenArgs) {
        val factory = SendThorchainModule.Factory(args.wallet, args.address, args.hideAddress, args.memo)
        val sendThorchainViewModel = viewModel<SendThorchainViewModel>(factory = factory)
        SendThorchainScreen(
            args.title,
            args.navigation,
            sendThorchainViewModel,
            args.amountInputModeViewModel,
            args.sendEntryPointDestId,
            args.amount,
            args.memo,
            riskyAddress = args.riskyAddress,
        )
    }
}
