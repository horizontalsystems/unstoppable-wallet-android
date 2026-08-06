package io.horizontalsystems.walletkit.chain.solana

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.adapters.SolanaAdapter
import io.horizontalsystems.walletkit.core.adapters.SolanaTransactionConverter
import io.horizontalsystems.walletkit.core.adapters.SolanaTransactionsAdapter
import io.horizontalsystems.walletkit.core.adapters.SplAdapter
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.chain.ChainSendScreenArgs
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.managers.SolanaKitManager
import io.horizontalsystems.walletkit.core.managers.SolanaRpcSourceManager
import io.horizontalsystems.walletkit.core.managers.SolanaWalletManager
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.addtoken.AddSolanaTokenBlockchainService
import io.horizontalsystems.walletkit.modules.addtoken.AddTokenModule
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.blockchainsettings.BlockchainSettingsModule
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceSolana
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.modules.send.solana.SendSolanaModule
import io.horizontalsystems.walletkit.modules.send.solana.SendSolanaScreen
import io.horizontalsystems.walletkit.modules.send.solana.SendSolanaViewModel
import io.horizontalsystems.walletkit.modules.solananetwork.SolanaNetworkPage
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.flow.Flow

class SolanaChainPlugin(
    private val alchemyApiKey: () -> String,
    private val jupiterApiKey: () -> String,
) : ChainPlugin {

    override val blockchainType: BlockchainType = BlockchainType.Solana

    val rpcSourceManager by lazy {
        SolanaRpcSourceManager(App.blockchainSettingsStorage, App.marketKit, alchemyApiKey())
    }

    val kitManager by lazy {
        SolanaKitManager(
            rpcSourceManager,
            SolanaWalletManager(App.walletManager, App.accountManager, App.marketKit),
            App.backgroundManager,
        )
    }

    override fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter? =
        when (val tokenType = wallet.token.type) {
            TokenType.Native -> SolanaAdapter(kitManager.getSolanaKitWrapper(wallet.account))
            is TokenType.Spl -> SplAdapter(kitManager.getSolanaKitWrapper(wallet.account), wallet, tokenType.address)
            else -> null
        }

    override fun createTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val solanaKitWrapper = kitManager.getSolanaKitWrapper(source.account)
        val baseToken = App.coinManager.getToken(TokenQuery(BlockchainType.Solana, TokenType.Native)) ?: return null
        val converter = SolanaTransactionConverter(App.coinManager, source, baseToken, solanaKitWrapper)

        return SolanaTransactionsAdapter(solanaKitWrapper, converter)
    }

    override fun unlink(account: Account) {
        kitManager.unlink(account)
    }

    override fun clearAccountData(accountId: String) {
        SolanaAdapter.clear(accountId)
    }

    override val walletReloadTrigger: Flow<*>
        get() = kitManager.kitStoppedFlow

    // The settings row shows the current RPC source, so it refreshes on source changes
    // (the kit-stopped signal only fires when a running kit restarts).
    override val settingsRefreshTrigger: Flow<*>
        get() = rpcSourceManager.rpcSourceUpdateFlow

    override fun statusInfo(): Map<String, Any>? = kitManager.statusInfo

    override suspend fun refreshKit() {
        kitManager.solanaKitWrapper?.solanaKit?.refresh()
    }

    override fun networkSettingsPage(): HSPage = SolanaNetworkPage

    override fun blockchainSettingsItem(): BlockchainSettingsModule.BlockchainItem.Chain? {
        val blockchain = rpcSourceManager.blockchain ?: return null
        return BlockchainSettingsModule.BlockchainItem.Chain(
            blockchain = blockchain,
            subtitle = rpcSourceManager.rpcSource.name,
            btcLike = false,
            page = SolanaNetworkPage,
            statEvent = StatEvent.Open(StatPage.BlockchainSettingsSolana),
        )
    }

    override suspend fun swapDestinationAddress(account: Account): String? =
        kitManager.getAddress(account.type)

    override fun sendTransactionService(token: Token): AbstractSendTransactionService =
        SendTransactionServiceSolana(token)

    override fun addressHandlers(): List<IAddressHandler> = listOf(AddressHandlerSolana())

    override fun addressValidator(token: Token): EnterAddressValidator = SolanaAddressValidator()

    override val supportsCustomTokens: Boolean get() = true

    override fun addTokenBlockchainService(blockchain: Blockchain): AddTokenModule.IAddTokenBlockchainService =
        AddSolanaTokenBlockchainService.getInstance(blockchain, jupiterApiKey())

    override fun backupSyncSourceName(): String = rpcSourceManager.rpcSource.name

    @Composable
    override fun SendScreen(args: ChainSendScreenArgs) {
        val factory = SendSolanaModule.Factory(args.wallet, args.address, args.hideAddress)
        val sendSolanaViewModel = viewModel<SendSolanaViewModel>(factory = factory)
        SendSolanaScreen(
            title = args.title,
            navigation = args.navigation,
            viewModel = sendSolanaViewModel,
            amountInputModeViewModel = args.amountInputModeViewModel,
            sendEntryPointDestId = args.sendEntryPointDestId,
            amount = args.amount,
            riskyAddress = args.riskyAddress,
        )
    }
}
