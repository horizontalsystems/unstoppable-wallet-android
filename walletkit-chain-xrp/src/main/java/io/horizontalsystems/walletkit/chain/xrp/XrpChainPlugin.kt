package io.horizontalsystems.walletkit.chain.xrp

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.xrpkit.XrpKit
import io.horizontalsystems.xrpkit.network.Network
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.adapters.XrpAdapter
import io.horizontalsystems.walletkit.core.adapters.XrpTokenAdapter
import io.horizontalsystems.walletkit.core.adapters.XrpTransactionsAdapter
import io.horizontalsystems.walletkit.core.ISendXrpAdapter
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.chain.ChainSendScreenArgs
import io.horizontalsystems.walletkit.core.chain.SendChainSettings
import io.horizontalsystems.walletkit.core.factories.XrpTransactionConverter
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.managers.XrpAccountManager
import io.horizontalsystems.walletkit.core.managers.XrpKitManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.addtoken.AddXrpTokenBlockchainService
import io.horizontalsystems.walletkit.modules.addtoken.AddTokenModule
import io.horizontalsystems.walletkit.modules.address.AddressHandlerXrp
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.balance.BalanceModule
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceXrp
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.receive.ReceiveActivatableTokenScreen
import io.horizontalsystems.walletkit.core.TokenActivationInfo
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.receive.ReceiveScreen
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.XrpAddressValidator
import io.horizontalsystems.walletkit.modules.send.xrp.SendXrpModule
import io.horizontalsystems.walletkit.modules.send.xrp.SendXrpScreen
import io.horizontalsystems.walletkit.modules.send.xrp.SendXrpViewModel
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import java.math.BigDecimal
import kotlin.reflect.KClass

class XrpChainPlugin : ChainPlugin {

    override val blockchainType: BlockchainType = BlockchainType.Xrp

    val kitManager by lazy { XrpKitManager(App.backgroundManager) }

    private val accountManager by lazy {
        XrpAccountManager(App.accountManager, App.walletManager, kitManager, App.tokenAutoEnableManager)
    }

    override suspend fun onAppStart() {
        accountManager.start()
    }

    override fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter? =
        when (val tokenType = wallet.token.type) {
            TokenType.Native -> XrpAdapter(kitManager.getKitWrapper(wallet.account))
            is TokenType.XrpAsset -> XrpTokenAdapter(kitManager.getKitWrapper(wallet.account), tokenType.currency, tokenType.issuer)
            else -> null
        }

    override val supportsCustomTokens: Boolean get() = true

    override fun addTokenBlockchainService(blockchain: Blockchain): AddTokenModule.IAddTokenBlockchainService =
        AddXrpTokenBlockchainService(blockchain)

    override val hasReceiveScreen: Boolean get() = true

    override fun tokenActivationInfo(wallet: Wallet): TokenActivationInfo? =
        if (wallet.token.type is TokenType.XrpAsset) activationInfo(wallet) else null

    private fun activationInfo(wallet: Wallet): TokenActivationInfo {
        val reserve = App.adapterManager.getAdapterForWallet<XrpTokenAdapter>(wallet)?.activationReserve
            ?: java.math.BigDecimal("0.2")
        return TokenActivationInfo(
            dialogDescriptionRes = R.string.ActivationRequired_Xrp_DialogDescription,
            insufficientBalanceDescriptionRes = R.string.Activate_Xrp_InsufficientBalance_Description,
            reserveNote = Translator.getString(R.string.Activate_Xrp_ReserveNote, "${reserve.stripTrailingZeros().toPlainString()} XRP"),
        )
    }

    @Composable
    override fun ReceiveScreen(navigation: HSNavigation, wallet: Wallet, receiveEntryPointDestId: KClass<out HSPage>?) {
        if (wallet.token.type is TokenType.XrpAsset) {
            ReceiveActivatableTokenScreen(navigation, wallet, receiveEntryPointDestId, activationInfo(wallet))
        } else {
            ReceiveScreen(navigation, wallet, receiveEntryPointDestId, false)
        }
    }

    override fun clearAccountData(accountId: String) {
        XrpKit.clear(App.instance, Network.MainNet, accountId)
    }

    override fun createTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val wrapper = kitManager.getKitWrapper(source.account)
        val baseToken = App.coinManager.getToken(TokenQuery(BlockchainType.Xrp, TokenType.Native)) ?: return null
        val converter = XrpTransactionConverter(source, wrapper.kit.receiveAddress, App.coinManager, baseToken)
        return XrpTransactionsAdapter(wrapper, converter)
    }

    override fun unlink(account: Account) {
        kitManager.unlink(account)
    }

    override fun statusInfo(): Map<String, Any>? = kitManager.statusInfo

    override suspend fun refreshKit() {
        kitManager.kitWrapper?.kit?.refresh()
    }

    override suspend fun swapDestinationAddress(account: Account): String =
        kitManager.getAddress(account.type)

    override fun sendTransactionService(token: Token): AbstractSendTransactionService =
        SendTransactionServiceXrp(token)

    override fun addressHandlers(): List<IAddressHandler> = listOf(AddressHandlerXrp())

    override fun addressValidator(token: Token): EnterAddressValidator = XrpAddressValidator(token)

    /** An unfunded account cannot receive less than the base reserve; say so on the balance row. */
    override suspend fun balanceWarning(wallet: Wallet): BalanceModule.BalanceWarning? {
        val adapter = App.adapterManager.getAdapterForWallet<XrpAdapter>(wallet) ?: return null
        return if (!adapter.isAccountActivated) {
            BalanceModule.BalanceWarning.XrpInactiveAccountWarning
        } else {
            null
        }
    }

    override fun sendAvailableBalance(token: Token, settings: SendChainSettings?): BigDecimal? =
        if (token.type == TokenType.Native) {
            App.adapterManager.getAdapterForToken<ISendXrpAdapter>(token)?.maxSendableBalance
                ?.coerceAtLeast(BigDecimal.ZERO)
        } else {
            null
        }

    // An X-address packs the destination tag with the account, so the tag travels with the
    // address; a classic address carries none. XRP takes no memo on the unified send screen.
    override fun sendTransactionData(
        token: Token,
        amount: BigDecimal,
        address: String,
        memo: String?,
        settings: SendChainSettings?,
    ) = SendTransactionData.Xrp(
        address = address,
        amount = amount,
        destinationTag = XrpKit.decodeXAddress(address)?.second,
    )

    @Composable
    override fun SendScreen(args: ChainSendScreenArgs) {
        val factory = SendXrpModule.Factory(args.wallet, args.address, args.hideAddress)
        val sendViewModel = viewModel<SendXrpViewModel>(factory = factory)
        SendXrpScreen(
            args.title,
            args.navigation,
            sendViewModel,
            args.amountInputModeViewModel,
            args.sendEntryPointDestId,
            args.amount,
            riskyAddress = args.riskyAddress,
        )
    }
}
