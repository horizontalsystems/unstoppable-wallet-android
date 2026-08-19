package io.horizontalsystems.walletkit.chain.ton

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.adapters.JettonAdapter
import io.horizontalsystems.walletkit.core.adapters.TonAdapter
import io.horizontalsystems.walletkit.core.adapters.TonTransactionConverter
import io.horizontalsystems.walletkit.core.adapters.TonTransactionsAdapter
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.chain.ChainSendScreenArgs
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.managers.TonAccountManager
import io.horizontalsystems.walletkit.core.managers.TonConnectManager
import io.horizontalsystems.walletkit.core.managers.TonKitManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.addtoken.AddTokenModule
import io.horizontalsystems.walletkit.modules.addtoken.AddTonTokenBlockchainService
import io.horizontalsystems.walletkit.modules.address.AddressHandlerTon
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceTon
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.ResultEffect
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.TonAddressValidator
import io.horizontalsystems.walletkit.modules.send.ton.SendTonModule
import io.horizontalsystems.walletkit.modules.send.ton.SendTonScreen
import io.horizontalsystems.walletkit.modules.send.ton.SendTonViewModel
import io.horizontalsystems.walletkit.modules.tonconnect.TonConnectNewPage
import io.horizontalsystems.walletkit.modules.tonconnect.TonConnectSendRequestPage
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.delay
import java.util.UUID

class TonChainPlugin(
    private val appName: String = "unstoppable",
) : ChainPlugin {

    companion object {
        val tonConnectManager: TonConnectManager
            get() = (ChainRegistry[BlockchainType.Ton] as TonChainPlugin).connectManager
    }

    override val blockchainType: BlockchainType = BlockchainType.Ton

    val kitManager by lazy { TonKitManager(App.backgroundManager) }

    val connectManager by lazy {
        TonConnectManager(App.instance, appName, App.appConfigProvider.appVersion)
    }

    private val accountManager by lazy {
        TonAccountManager(App.accountManager, App.walletManager, kitManager, App.tokenAutoEnableManager)
    }

    override suspend fun onAppStart() {
        accountManager.start()
        connectManager.start()
    }

    override fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter? =
        when (val tokenType = wallet.token.type) {
            TokenType.Native -> TonAdapter(kitManager.getTonKitWrapper(wallet.account))
            is TokenType.Jetton -> JettonAdapter(
                kitManager.getTonKitWrapper(wallet.account),
                tokenType.address,
                wallet,
            )
            else -> null
        }

    // Not implemented: ton-kit exposes no clear API, unlike bitcoin-kit and ethereum-kit which
    // provide Kit.clear(context, network, walletId). Deleting the store from here would mean
    // hardcoding the kit's internal database name, which silently stops working the moment the
    // kit renames it. So TON account data currently outlives the account — history and
    // addresses, not keys. Needs a clear() in ton-kit; see #9452.
    override fun clearAccountData(accountId: String) = Unit

    override fun createTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val tonKitWrapper = kitManager.getTonKitWrapper(source.account)
        val baseToken = App.coinManager.getToken(TokenQuery(BlockchainType.Ton, TokenType.Native)) ?: return null
        val converter = TonTransactionConverter(
            tonKitWrapper.tonKit.receiveAddress,
            App.coinManager,
            source,
            baseToken,
        )

        return TonTransactionsAdapter(tonKitWrapper, converter)
    }

    override fun unlink(account: Account) {
        kitManager.unlink(account)
    }

    override fun statusInfo(): Map<String, Any>? = kitManager.statusInfo

    override suspend fun refreshKit() {
        kitManager.tonKitWrapper?.tonKit?.refresh()
    }

    override suspend fun swapDestinationAddress(account: Account): String =
        kitManager.getAddress(account.type)

    override fun sendTransactionService(token: Token): AbstractSendTransactionService =
        SendTransactionServiceTon(token)

    override val supportsCustomTokens: Boolean get() = true

    override fun addTokenBlockchainService(blockchain: Blockchain): AddTokenModule.IAddTokenBlockchainService =
        AddTonTokenBlockchainService(blockchain)

    override fun addressHandlers(): List<IAddressHandler> = listOf(AddressHandlerTon())

    override fun addressValidator(token: Token): EnterAddressValidator = TonAddressValidator()

    override fun isDeepLinkSupported(link: String, fromScanner: Boolean): Boolean =
        if (fromScanner) {
            link.startsWith("tc:") || link.startsWith("https://unstoppable.money/ton-connect")
        } else {
            link.startsWith("unstoppable.money:") || link.startsWith("tc:")
        }

    override suspend fun handleDeepLink(link: String, closeApp: Boolean) {
        connectManager.handle(link, closeApp)
    }

    @Composable
    override fun MainScreenEffects(navigation: HSNavigation) {
        val activity = LocalActivity.current

        val sendRequest by connectManager.pendingSendRequest.collectAsState()
        LaunchedEffect(sendRequest) {
            if (sendRequest != null) {
                navigation.slideFromBottom(TonConnectSendRequestPage)
            }
        }

        val dappRequest by connectManager.pendingDappRequest.collectAsState()
        val uuid = rememberSaveable { UUID.randomUUID().toString() }
        var closeAppOnResult by remember { mutableStateOf(false) }
        ResultEffect<TonConnectNewPage.Result>(resultKeyUuid = uuid) { result ->
            if (closeAppOnResult) {
                if (result.approved) {
                    //Need delay to get connected before closing activity
                    delay(1000)
                }
                activity?.finish()
            }
        }

        LaunchedEffect(dappRequest) {
            val request = dappRequest ?: return@LaunchedEffect
            closeAppOnResult = request.closeAppOnResult
            val screen = TonConnectNewPage(request.dAppRequest)
            screen.resultKey = uuid
            navigation.slideFromBottom(screen)
            connectManager.onDappRequestHandled()
        }
    }

    @Composable
    override fun SendScreen(args: ChainSendScreenArgs) {
        val factory = SendTonModule.Factory(args.wallet, args.address, args.hideAddress)
        val sendTonViewModel = viewModel<SendTonViewModel>(factory = factory)
        SendTonScreen(
            args.title,
            args.navigation,
            sendTonViewModel,
            args.amountInputModeViewModel,
            args.sendEntryPointDestId,
            args.amount,
            riskyAddress = args.riskyAddress,
        )
    }
}
