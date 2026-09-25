package io.horizontalsystems.walletkit.chain.near

import androidx.compose.runtime.Composable
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.nearkit.NearKit
import io.horizontalsystems.nearkit.network.Network
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.ISendNearAdapter
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.adapters.NearAdapter
import io.horizontalsystems.walletkit.core.adapters.NearTokenAdapter
import io.horizontalsystems.walletkit.core.adapters.NearTransactionsAdapter
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.chain.SendChainSettings
import io.horizontalsystems.walletkit.core.chain.SendMemoSupport
import io.horizontalsystems.walletkit.core.factories.NearTransactionConverter
import io.horizontalsystems.walletkit.core.managers.NearAccountManager
import io.horizontalsystems.walletkit.core.managers.NearKitManager
import io.horizontalsystems.walletkit.core.managers.NearRpcSourceManager
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.modules.blockchainsettings.BlockchainSettingsModule
import io.horizontalsystems.walletkit.modules.nearnetwork.NearNetworkPage
import kotlinx.coroutines.flow.Flow
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.address.AddressHandlerNear
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.addtoken.AddNearTokenBlockchainService
import io.horizontalsystems.walletkit.modules.addtoken.AddTokenModule
import io.horizontalsystems.walletkit.modules.memo.MemoVisibility
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceNear
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.receive.ReceiveScreen
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.modules.send.address.NearAddressValidator
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import java.math.BigDecimal
import kotlin.reflect.KClass

class NearChainPlugin : ChainPlugin {

    override val blockchainType: BlockchainType = BlockchainType.Near

    val rpcSourceManager by lazy {
        NearRpcSourceManager(App.blockchainSettingsStorage, App.marketKit)
    }

    val kitManager by lazy { NearKitManager(App.backgroundManager, rpcSourceManager) }

    private val accountManager by lazy {
        NearAccountManager(App.accountManager, App.walletManager, kitManager, App.tokenAutoEnableManager, App.coinManager)
    }

    override suspend fun onAppStart() {
        accountManager.start()
    }

    override fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter? =
        when (val tokenType = wallet.token.type) {
            TokenType.Native -> NearAdapter(kitManager.getKitWrapper(wallet.account))
            is TokenType.Nep141 -> NearTokenAdapter(kitManager.getKitWrapper(wallet.account), tokenType.contractId, wallet.token.decimals)
            else -> null
        }

    override val supportsCustomTokens: Boolean get() = true

    override fun addTokenBlockchainService(blockchain: Blockchain): AddTokenModule.IAddTokenBlockchainService =
        AddNearTokenBlockchainService(blockchain)

    override val hasReceiveScreen: Boolean get() = true

    @Composable
    override fun ReceiveScreen(navigation: HSNavigation, wallet: Wallet, receiveEntryPointDestId: KClass<out HSPage>?) {
        ReceiveScreen(navigation, wallet, receiveEntryPointDestId, false)
    }

    override fun clearAccountData(accountId: String) {
        NearKit.clear(App.instance, Network.MainNet, accountId)
    }

    override fun createTransactionsAdapter(source: TransactionSource): ITransactionsAdapter? {
        val wrapper = kitManager.getKitWrapper(source.account)
        val baseToken = App.coinManager.getToken(TokenQuery(BlockchainType.Near, TokenType.Native)) ?: return null
        val converter = NearTransactionConverter(source, wrapper.kit, App.coinManager, baseToken)
        return NearTransactionsAdapter(wrapper, converter)
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

    override fun networkSettingsPage(): HSPage = NearNetworkPage

    override fun blockchainSettingsItem(): BlockchainSettingsModule.BlockchainItem.Chain? {
        val blockchain = rpcSourceManager.blockchain ?: return null
        return BlockchainSettingsModule.BlockchainItem.Chain(
            blockchain = blockchain,
            subtitle = rpcSourceManager.rpcSource.name,
            btcLike = false,
            page = NearNetworkPage,
            statEvent = StatEvent.Open(StatPage.BlockchainSettingsNear),
        )
    }

    override fun backupSyncSourceName(): String = rpcSourceManager.rpcSource.name

    override fun statusInfo(): Map<String, Any>? = kitManager.statusInfo

    override suspend fun refreshKit() {
        kitManager.kitWrapper?.kit?.refresh()
    }

    override suspend fun swapDestinationAddress(account: Account): String =
        kitManager.getAddress(account.type)

    override fun sendTransactionService(token: Token): AbstractSendTransactionService =
        SendTransactionServiceNear(token)

    override fun addressHandlers(): List<IAddressHandler> = listOf(AddressHandlerNear())

    override fun addressValidator(token: Token): EnterAddressValidator = NearAddressValidator(token)

    override fun addressValidator(
        token: Token,
        allowOwnAddress: Boolean,
        transparentOnly: Boolean,
    ): EnterAddressValidator = NearAddressValidator(token, allowOwnAddress)

    /** Only NEP-141 transfers carry a memo (`ft_transfer`); a native NEAR transfer has none. */
    override suspend fun sendMemoSupport(token: Token, address: String?): SendMemoSupport? =
        if (token.type is TokenType.Nep141) SendMemoSupport(maxBytes = MEMO_MAX_BYTES, visibility = MemoVisibility.Public) else null

    override fun sendAvailableBalance(token: Token, settings: SendChainSettings?): BigDecimal? =
        if (token.type == TokenType.Native) {
            App.adapterManager.getAdapterForToken<ISendNearAdapter>(token)?.maxSendableBalance
        } else {
            null
        }

    override fun sendTransactionData(
        token: Token,
        amount: BigDecimal,
        address: String,
        memo: String?,
        extraInput: String?,
        settings: SendChainSettings?,
    ) = SendTransactionData.Near(
        address = address,
        amount = amount,
        memo = memo?.takeIf { token.type is TokenType.Nep141 && it.isNotBlank() },
    )

    companion object {
        /** NEP-141 sets no limit; this keeps the transaction and its gas small. */
        private const val MEMO_MAX_BYTES = 256
    }
}
