package io.horizontalsystems.walletkit.chain.monero

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.adapters.MoneroAdapter
import io.horizontalsystems.walletkit.core.adapters.toMoneroSeed
import io.horizontalsystems.walletkit.core.chain.ChainKeyRow
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.chain.ChainSendScreenArgs
import io.horizontalsystems.walletkit.core.managers.MoneroBirthdayProvider
import io.horizontalsystems.walletkit.core.managers.MoneroNodeManager
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.managers.statusInfo
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.blockchainsettings.BlockchainSettingsModule
import io.horizontalsystems.walletkit.modules.manageaccount.showmonerokey.ShowMoneroKeyModule
import io.horizontalsystems.walletkit.modules.manageaccount.showmonerokey.ShowMoneroKeyPage
import io.horizontalsystems.walletkit.modules.moneronetwork.MoneroNetworkPage
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceMonero
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.receive.monero.ReceiveMoneroScreen
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.modules.send.monero.SendMoneroModule
import io.horizontalsystems.walletkit.modules.send.monero.SendMoneroScreen
import io.horizontalsystems.walletkit.modules.send.monero.SendMoneroViewModel
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.monerokit.MoneroKit
import io.horizontalsystems.monerokit.MoneroMnemonic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Date
import kotlin.reflect.KClass

class MoneroChainPlugin(
    private val context: () -> Context,
    private val moneroNodeManager: () -> MoneroNodeManager,
) : ChainPlugin {

    override val blockchainType: BlockchainType = BlockchainType.Monero

    private val birthdayProvider by lazy { MoneroBirthdayProvider() }

    override fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter? {
        val nodeManager = moneroNodeManager()
        if (nodeManager.isResolvingFastestNode) {
            // Defer creation until startup Auto-Select picks the fastest node, so the
            // adapter connects once to it instead of reconnecting. reloadWallets(Monero)
            // recreates the adapter when resolution finishes.
            return null
        }
        return MoneroAdapter.create(
            context = context(),
            wallet = wallet,
            restoreSettings = restoreSettings,
            node = nodeManager.currentNode,
        )
    }

    override fun clearAccountData(accountId: String) {
        MoneroAdapter.clear(accountId)
    }

    override suspend fun onAppStart() {
        val nodeManager = moneroNodeManager()
        nodeManager.nodePinger = { serialized ->
            MoneroKit.pingNodes(context(), serialized).map {
                MoneroNodeManager.NodePingResult(it.serialized, it.isValid, it.responseTime)
            }
        }
        nodeManager.autoSelectFastestNodeOnStartup()
    }

    override fun addressHandlers(): List<IAddressHandler> = listOf(AddressHandlerMonero())

    override fun addressValidator(token: Token): EnterAddressValidator = MoneroAddressValidator()

    override fun newWalletBirthdayHeight(): Long = birthdayProvider.restoreHeightForNewWallet()

    override fun firstBlockDate(): LocalDate = LocalDate.of(2014, 4, 18)

    override suspend fun estimateBlockDate(height: Long): Date? = MoneroKit.dateForRestoreHeight(height)

    override suspend fun estimateBlockHeightFromDate(date: Date): Long = MoneroKit.restoreHeightForDate(date)

    override val walletReloadTrigger: Flow<*>
        get() = moneroNodeManager().currentNodeUpdatedFlow

    override fun statusInfo(): Map<String, Any>? {
        val wallet = App.walletManager.activeWallets
            .firstOrNull { it.token.blockchainType == BlockchainType.Monero } ?: return null
        return App.adapterManager.getAdapterForWallet<MoneroAdapter>(wallet)?.statusInfo
    }

    override fun networkSettingsPage(): HSPage = MoneroNetworkPage

    override fun blockchainSettingsItem(): BlockchainSettingsModule.BlockchainItem.Chain? {
        val nodeManager = moneroNodeManager()
        val blockchain = nodeManager.blockchain ?: return null
        return BlockchainSettingsModule.BlockchainItem.Chain(
            blockchain = blockchain,
            subtitle = nodeManager.currentNode.name,
            btcLike = true,
            page = MoneroNetworkPage,
            statEvent = StatEvent.OpenBlockchainSettingsCryptoNote(blockchain.uid),
        )
    }

    override suspend fun swapDestinationAddress(account: Account): String? =
        withContext(Dispatchers.IO) {
            MoneroKit.getAddress(account.type.toMoneroSeed(), 0, 1)
        }

    override fun sendTransactionService(token: Token): AbstractSendTransactionService {
        val adapter = App.adapterManager.getAdapterForToken<MoneroAdapter>(token)
            ?: throw IllegalStateException("MoneroAdapter is null")
        return SendTransactionServiceMonero(adapter)
    }

    @Composable
    override fun SendScreen(args: ChainSendScreenArgs) {
        val factory = SendMoneroModule.Factory(args.wallet, args.address, args.hideAddress)
        val sendMoneroViewModel = viewModel<SendMoneroViewModel>(factory = factory)
        SendMoneroScreen(
            args.title,
            args.navigation,
            sendMoneroViewModel,
            args.amountInputModeViewModel,
            args.sendEntryPointDestId,
            args.amount,
            args.memo,
            riskyAddress = args.riskyAddress,
        )
    }

    override val hasReceiveScreen: Boolean get() = true

    @Composable
    override fun ReceiveScreen(navigation: HSNavigation, wallet: Wallet, receiveEntryPointDestId: KClass<out HSPage>?) {
        ReceiveMoneroScreen(navigation, wallet, receiveEntryPointDestId)
    }

    override fun validateWatchViewKey(viewKey: String, address: String) {
        MoneroKit.validatePrivateViewKey(viewKey, address)
    }

    override fun altMnemonicSuggestions(word: String): List<String> = MoneroMnemonic.suggestions(word)

    override fun isAltMnemonicWord(word: String, partial: Boolean): Boolean =
        MoneroMnemonic.isValidWord(word, partial)

    override val altMnemonicWordCount: Int get() = MoneroMnemonic.WORD_COUNT

    override fun buildAltMnemonicAccount(words: List<String>, passphrase: String): AccountType? = try {
        MoneroMnemonic.validateChecksum(words)
        // The passphrase is wallet2's seed offset. A whitespace-only value would derive a
        // real offset wallet, yet local backup drops it (isNotBlank) and restores it as "",
        // silently yielding a different wallet — collapse blank to empty so every consumer
        // (backup, stats, description) agrees the offset is absent.
        AccountType.MoneroMnemonic(words, passphrase.ifBlank { "" })
    } catch (e: Exception) {
        null
    }

    override fun privateKeyRows(account: Account): List<ChainKeyRow> =
        ShowMoneroKeyModule.getPrivateMoneroKeys(account)?.let { keys ->
            listOf(
                ChainKeyRow(
                    titleRes = R.string.PrivateKeys_MoneroPrivateKey,
                    descriptionRes = R.string.PrivateKeys_MoneroPrivateKeyDescription,
                    page = ShowMoneroKeyPage(ShowMoneroKeyPage.Input(keys)),
                    statPage = StatPage.MoneroPrivateKey,
                )
            )
        }.orEmpty()

    override fun publicKeyRows(account: Account): List<ChainKeyRow> =
        ShowMoneroKeyModule.getPublicMoneroKeys(account)?.let { keys ->
            listOf(
                ChainKeyRow(
                    titleRes = R.string.PublicKeys_MoneroPublicKey,
                    descriptionRes = R.string.PublicKeys_MoneroPublicKeyDescription,
                    page = ShowMoneroKeyPage(ShowMoneroKeyPage.Input(keys)),
                    statPage = StatPage.MoneroPublicKey,
                )
            )
        }.orEmpty()

    @Composable
    override fun TokenBalanceExtraCells(wallet: Wallet, navigation: HSNavigation) {
        MoneroAccountCell(wallet, navigation)
    }
}
