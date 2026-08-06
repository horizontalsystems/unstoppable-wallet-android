package io.horizontalsystems.walletkit.chain.stellar

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.adapters.StellarAdapter
import io.horizontalsystems.walletkit.core.adapters.StellarAssetAdapter
import io.horizontalsystems.walletkit.core.adapters.StellarTransactionsAdapter
import io.horizontalsystems.walletkit.core.chain.ChainKeyRow
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.chain.ChainSendScreenArgs
import io.horizontalsystems.walletkit.core.factories.StellarTransactionConverter
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.managers.StellarAccountManager
import io.horizontalsystems.walletkit.core.managers.StellarKitManager
import io.horizontalsystems.walletkit.core.managers.toStellarWallet
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.manageaccount.stellarsecretkey.StellarSecretKeyPage
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.StellarSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceStellar
import io.horizontalsystems.walletkit.modules.address.AddressHandlerStellar
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.receive.ReceiveScreen
import io.horizontalsystems.walletkit.modules.receive.ReceiveStellarAssetScreen
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.StellarAddressValidator
import io.horizontalsystems.walletkit.modules.send.stellar.SendStellarModule
import io.horizontalsystems.walletkit.modules.send.stellar.SendStellarScreen
import io.horizontalsystems.walletkit.modules.send.stellar.SendStellarViewModel
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.walletkit.modules.walletconnect.handler.IWCHandler
import io.horizontalsystems.walletkit.modules.walletconnect.stellar.WCHandlerStellar
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.stellarkit.StellarKit
import kotlin.reflect.KClass

class StellarChainPlugin : ChainPlugin {

    override val blockchainType: BlockchainType = BlockchainType.Stellar

    val kitManager by lazy { StellarKitManager(App.backgroundManager) }

    private val accountManager by lazy {
        StellarAccountManager(App.accountManager, App.walletManager, kitManager, App.tokenAutoEnableManager)
    }

    override suspend fun onAppStart() {
        accountManager.start()
    }

    override fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter? =
        when (val tokenType = wallet.token.type) {
            TokenType.Native -> StellarAdapter(kitManager.getStellarKitWrapper(wallet.account))
            is TokenType.Asset -> StellarAssetAdapter(
                kitManager.getStellarKitWrapper(wallet.account),
                tokenType.code,
                tokenType.issuer,
            )
            else -> null
        }

    override fun createTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val stellarKitWrapper = kitManager.getStellarKitWrapper(source.account)
        val baseToken = App.coinManager.getToken(TokenQuery(BlockchainType.Stellar, TokenType.Native)) ?: return null
        val converter = StellarTransactionConverter(
            source,
            stellarKitWrapper.stellarKit.receiveAddress,
            App.coinManager,
            baseToken,
        )

        return StellarTransactionsAdapter(stellarKitWrapper, converter)
    }

    override fun unlink(account: Account) {
        kitManager.unlink(account)
    }

    override fun statusInfo(): Map<String, Any>? = kitManager.statusInfo

    override suspend fun refreshKit() {
        kitManager.stellarKitWrapper?.stellarKit?.refresh()
    }

    override suspend fun swapDestinationAddress(account: Account): String =
        kitManager.getAddress(account.type)

    override fun sendTransactionService(token: Token): AbstractSendTransactionService {
        val activeAccount = App.accountManager.activeAccount!!
        val stellarKitWrapper = kitManager.getStellarKitWrapper(activeAccount)
        return SendTransactionServiceStellar(stellarKitWrapper.stellarKit, token)
    }

    override fun wcHandlers(): List<IWCHandler> = listOf(WCHandlerStellar(kitManager))

    override fun swapProviders(): List<IMultiSwapProvider> = listOf(StellarSwapProvider())

    override fun privateKeyAccountTypes(privateKey: String): List<AccountType> =
        if (StellarKit.isValidSecretKey(privateKey)) {
            listOf(AccountType.StellarSecretKey(privateKey))
        } else {
            emptyList()
        }

    override fun privateKeyRows(account: Account): List<ChainKeyRow> {
        val secretKey = try {
            StellarKit.getSecretSeed(account.type.toStellarWallet())
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        return listOf(
            ChainKeyRow(
                titleRes = R.string.PrivateKeys_StellarSecretKey,
                descriptionRes = R.string.PrivateKeys_StellarSecretKeyDescription,
                page = StellarSecretKeyPage(StellarSecretKeyPage.Input(secretKey)),
                statPage = StatPage.StellarSecretKey,
            )
        )
    }

    override fun addressHandlers(): List<IAddressHandler> = listOf(AddressHandlerStellar())

    override fun addressValidator(token: Token): EnterAddressValidator = StellarAddressValidator(token)

    override val hasReceiveScreen: Boolean get() = true

    @Composable
    override fun ReceiveScreen(navigation: HSNavigation, wallet: Wallet, receiveEntryPointDestId: KClass<out HSPage>?) {
        if (wallet.token.type is TokenType.Asset) {
            ReceiveStellarAssetScreen(navigation, wallet, receiveEntryPointDestId)
        } else {
            ReceiveScreen(navigation, wallet, receiveEntryPointDestId, false)
        }
    }

    @Composable
    override fun SendScreen(args: ChainSendScreenArgs) {
        val factory = SendStellarModule.Factory(args.wallet, args.address, args.hideAddress)
        val sendStellarViewModel = viewModel<SendStellarViewModel>(factory = factory)
        SendStellarScreen(
            args.title,
            args.navigation,
            sendStellarViewModel,
            args.amountInputModeViewModel,
            args.sendEntryPointDestId,
            args.amount,
            riskyAddress = args.riskyAddress,
        )
    }
}
