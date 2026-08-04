package io.horizontalsystems.walletkit.chain.zano

import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.adapters.ZanoAdapter
import io.horizontalsystems.walletkit.core.chain.ChainPlugin
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.managers.ZanoKitManager
import io.horizontalsystems.walletkit.core.managers.ZanoNodeManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.Wallet
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.core.chain.ChainSendScreenArgs
import io.horizontalsystems.walletkit.core.managers.statusInfo
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.modules.blockchainsettings.BlockchainSettingsModule
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.send.zano.SendZanoModule
import io.horizontalsystems.walletkit.modules.send.zano.SendZanoScreen
import io.horizontalsystems.walletkit.modules.send.zano.SendZanoViewModel
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.modules.zanonetwork.ZanoNetworkPage
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
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
    private val zanoKitManager: () -> ZanoKitManager,
) : ChainPlugin {

    override val blockchainType: BlockchainType = BlockchainType.Zano

    override fun createAdapter(wallet: Wallet, restoreSettings: RestoreSettings): IAdapter =
        ZanoAdapter.create(
            wallet = wallet,
            zanoKitManager = zanoKitManager(),
            restoreSettings = restoreSettings,
        )

    override fun unlink(account: Account) {
        zanoKitManager().unlink(account)
    }

    override fun addressHandlers(): List<IAddressHandler> = listOf(AddressHandlerZano())

    override fun domainAddressHandlers(): List<IAddressHandler> =
        listOf(AddressHandlerZanoAlias(ZanoAliasResolver(zanoNodeManager())))

    override fun addressValidator(token: Token): EnterAddressValidator = ZanoAddressValidator()

    override fun newWalletBirthdayHeight(): Long = ZanoKit.restoreHeightForDate(Date())

    override fun firstBlockDate(): LocalDate = LocalDate.of(2019, 5, 8)

    override suspend fun estimateBlockDate(height: Long): Date? = ZanoKit.dateForRestoreHeight(height)

    override suspend fun estimateBlockHeightFromDate(date: Date): Long = ZanoKit.restoreHeightForDate(date)

    override val walletReloadTrigger: Flow<*>
        get() = zanoNodeManager().currentNodeUpdatedFlow

    override fun statusInfo(): Map<String, Any>? = zanoKitManager().statusInfo

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

    @Composable
    override fun SendScreen(args: ChainSendScreenArgs) {
        val factory = SendZanoModule.Factory(args.wallet, args.address, args.hideAddress)
        val sendZanoViewModel = viewModel<SendZanoViewModel>(factory = factory)
        SendZanoScreen(
            args.title,
            args.navigation,
            sendZanoViewModel,
            args.amountInputModeViewModel,
            args.sendEntryPointDestId,
            args.amount,
            args.memo,
            riskyAddress = args.riskyAddress,
        )
    }
}
