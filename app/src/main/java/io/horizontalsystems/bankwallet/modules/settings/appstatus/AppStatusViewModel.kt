package io.horizontalsystems.bankwallet.modules.settings.appstatus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.AppLog
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.adapters.BaseTronAdapter
import io.horizontalsystems.bankwallet.core.adapters.BitcoinBaseAdapter
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.settings.appstatus.AppStatusModule.BlockContent
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch
import java.util.Date

class AppStatusViewModel(
    private val systemInfoManager: ISystemInfoManager,
    private val localStorage: ILocalStorage,
    private val accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val adapterManager: IAdapterManager,
    private val marketKit: MarketKitWrapper
) : ViewModel() {

    private var blockViewItems: List<AppStatusModule.BlockData> = emptyList()
    private var appStatusAsText: String? = null
    private val appLogs = AppLog.getLog()

    var uiState by mutableStateOf(
        AppStatusModule.UiState(
            appStatusAsText = appStatusAsText,
            blockViewItems = blockViewItems
        )
    )
        private set

    init {
        viewModelScope.launch {
            appStatusAsText = formatMapToString(getStatusMap())

            blockViewItems = listOf<AppStatusModule.BlockData>()
                .plus(getAppInfoBlock())
                .plus(getVersionHistoryBlock())
                .plus(getWalletsStatusBlock())
                .plus(getBlockchainStatusBlock())
                .plus(getMarketLastSyncTimestampsBlock())
                .plus(getAppLogBlocks())

            sync()
        }
    }

    private fun sync() {
        uiState = AppStatusModule.UiState(
            appStatusAsText = appStatusAsText,
            blockViewItems = blockViewItems
        )
    }

    private fun getStatusMap(): LinkedHashMap<String, Any> {
        val status = LinkedHashMap<String, Any>()

        status["App Info"] = getAppInfo()
        status["Version History"] = getVersionHistory()
        status["Wallets Status"] = getWalletsStatus()
        status["Blockchain Status"] = getBlockchainStatus()
        status["App Log"] = appLogs
        status["Market Last Sync Timestamps"] = getMarketLastSyncTimestamps()

        return status
    }

    private fun getAppLogBlocks(): List<AppStatusModule.BlockData> {
        val blocks = mutableListOf<AppStatusModule.BlockData>()
        var sectionTitleNotSet = true
        appLogs.forEach { (key, value) ->
            val title = if (sectionTitleNotSet) "App Log" else null
            val map = mapOf("" to value)
            val content = formatMapToString(map)?.removePrefix(":")
                ?.removePrefix("\n")
                ?.trimEnd() ?: ""
            val item = AppStatusModule.BlockData(
                title = title,
                content = listOf(
                    BlockContent.Header(key.replaceFirstChar(Char::uppercase)),
                    BlockContent.Text(content),
                )
            )
            blocks.add(item)
            sectionTitleNotSet = false
        }
        return blocks
    }

    private fun getVersionHistory(): Map<String, Any> {
        val versions = LinkedHashMap<String, Date>()

        localStorage.appVersions.sortedBy { it.timestamp }.forEach { version ->
            versions[version.version] = Date(version.timestamp)
        }
        return versions
    }

    private fun getVersionHistoryBlock(): AppStatusModule.BlockData {
        val versions = mutableListOf<BlockContent.TitleValue>()
        localStorage.appVersions.sortedBy { it.timestamp }.forEach { version ->
            versions.add(
                BlockContent.TitleValue(
                    DateHelper.formatDate(Date(version.timestamp), "MMM d, yyyy, HH:mm"),
                    version.version,
                )
            )
        }

        return AppStatusModule.BlockData("Version History", versions)
    }

    private fun getWalletsStatus(): Map<String, Any> {
        val wallets = LinkedHashMap<String, Any>()

        for (account in accountManager.accounts) {
            val title = account.name

            wallets[title] = getAccountDetails(account)
        }
        return wallets
    }

    private fun getWalletsStatusBlock(): List<AppStatusModule.BlockData> {
        val walletBlocks = mutableListOf<AppStatusModule.BlockData>()

        accountManager.accounts.forEachIndexed { index, account ->
            val title = if (index == 0) "Wallet Status" else null
            val origin = getAccountOrigin(account)

            walletBlocks.add(
                AppStatusModule.BlockData(
                    title,
                    listOf(
                        BlockContent.TitleValue("Name", account.name),
                        BlockContent.TitleValue("Origin", origin),
                        BlockContent.TitleValue("Type", account.type.description),
                    )
                )
            )
        }
        return walletBlocks
    }

    private fun getBlockchainStatus(): Map<String, Any> {
        val blockchainStatus = LinkedHashMap<String, Any>()
        val blockchainTypesToDisplay =
            listOf(
                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.Dash,
                BlockchainType.Litecoin,
                BlockchainType.ECash
            )

        walletManager.activeWallets
            .filter { blockchainTypesToDisplay.contains(it.token.blockchainType) }
            .sortedBy { it.token.coin.name }
            .forEach { wallet ->
                (adapterManager.getAdapterForWallet(wallet) as? BitcoinBaseAdapter)?.let { adapter ->
                    val statusTitle = "${wallet.token.coin.name}${wallet.badge?.let { "-$it" } ?: ""}"
                    blockchainStatus[statusTitle] = adapter.statusInfo
                }
            }

        walletManager.activeWallets.firstOrNull { it.token.blockchainType == BlockchainType.Tron }?.let { wallet ->
            (adapterManager.getAdapterForWallet(wallet) as? BaseTronAdapter)?.statusInfo?.let { statusInfo ->
                blockchainStatus["Tron"] = statusInfo
            }
        }
        return blockchainStatus
    }

    private fun getBlockchainStatusBlock(): List<AppStatusModule.BlockData> {
        val blocks = mutableListOf<AppStatusModule.BlockData>()
        val blockchainTypesToDisplay =
            listOf(
                BlockchainType.Bitcoin,
                BlockchainType.BitcoinCash,
                BlockchainType.Dash,
                BlockchainType.Litecoin,
                BlockchainType.ECash,
            )

        var sectionTitleNotSet = true

        walletManager.activeWallets
            .filter { blockchainTypesToDisplay.contains(it.token.blockchainType) }
            .sortedBy { it.token.coin.name }
            .forEach { wallet ->
                val title = if (sectionTitleNotSet) "Blockchain Status" else null
                (adapterManager.getAdapterForWallet(wallet) as? BitcoinBaseAdapter)?.let { adapter ->
                    val statusTitle = "${wallet.token.coin.name}${wallet.badge?.let { "-$it" } ?: ""}"

                    val item = AppStatusModule.BlockData(
                        title,
                        listOf(
                            BlockContent.TitleValue("Blockchain", statusTitle),
                            BlockContent.Text(formatMapToString(adapter.statusInfo)?.trimEnd() ?: ""),
                        )
                    )
                    blocks.add(item)
                    if (sectionTitleNotSet) {
                        sectionTitleNotSet = false
                    }
                }
            }

        walletManager.activeWallets.firstOrNull { it.token.blockchainType == BlockchainType.Tron }?.let { wallet ->
            val title = if (sectionTitleNotSet) "Blockchain Status" else null
            (adapterManager.getAdapterForWallet(wallet) as? BaseTronAdapter)?.statusInfo?.let { statusInfo ->
                val item = AppStatusModule.BlockData(
                    title,
                    listOf(
                        BlockContent.TitleValue("Blockchain", "Tron"),
                        BlockContent.Text(formatMapToString(statusInfo)?.trimEnd() ?: ""),
                    )
                )
                blocks.add(item)
                if (sectionTitleNotSet) {
                    sectionTitleNotSet = false
                }
            }
        }

        return blocks
    }

    private fun getMarketLastSyncTimestamps(): Map<String, Any> {
        val syncInfo = marketKit.syncInfo()
        val info = LinkedHashMap<String, Any>()
        info["Coins"] = syncInfo.coinsTimestamp ?: ""
        info["Blockchains"] = syncInfo.blockchainsTimestamp ?: ""
        info["Tokens"] = syncInfo.tokensTimestamp ?: ""

        return info
    }

    private fun getMarketLastSyncTimestampsBlock(): AppStatusModule.BlockData {
        val syncInfo = marketKit.syncInfo()

        return AppStatusModule.BlockData(
            title = "Market Last Sync Timestamps",
            content = listOf(
                BlockContent.TitleValue("Coins", syncInfo.coinsTimestamp ?: ""),
                BlockContent.TitleValue("Blockchains", syncInfo.blockchainsTimestamp ?: ""),
                BlockContent.TitleValue("Tokens", syncInfo.tokensTimestamp ?: ""),
            )
        )
    }

    private fun getAppInfo(): Map<String, Any> {
        val appInfo = LinkedHashMap<String, Any>()
        appInfo["Current Time"] = Date()
        appInfo["App Version"] = systemInfoManager.appVersion
        appInfo["Device Model"] = systemInfoManager.deviceModel
        appInfo["OS Version"] = systemInfoManager.osVersion

        return appInfo
    }

    private fun getAppInfoBlock(): AppStatusModule.BlockData {
        return AppStatusModule.BlockData(
            title = "App Info",
            content = listOf(
                BlockContent.TitleValue("Current Time", DateHelper.formatDate(Date(), "MMM d, yyyy, HH:mm")),
                BlockContent.TitleValue("App Version", systemInfoManager.appVersion),
                BlockContent.TitleValue("Device Model", systemInfoManager.deviceModel),
                BlockContent.TitleValue("OS Version", systemInfoManager.osVersion),
            )
        )
    }

    private fun getAccountDetails(account: Account): LinkedHashMap<String, Any> {
        val accountDetails = LinkedHashMap<String, Any>()

        accountDetails["Origin"] = getAccountOrigin(account)
        accountDetails["Type"] = account.type.description

        return accountDetails
    }

    private fun getAccountOrigin(account: Account): String {
        return if (account.isWatchAccount) "Watched" else account.origin.value
    }

    @Suppress("UNCHECKED_CAST")
    private fun formatMapToString(
        status: Map<String, Any>?,
        indentation: String = "",
        bullet: String = "",
        level: Int = 0
    ): String? {
        if (status == null)
            return null

        val sb = StringBuilder()
        status.toList().forEach { (key, value) ->
            val title = "$indentation$bullet$key"
            when (value) {
                is Date -> {
                    val date = DateHelper.formatDate(value, "MMM d, yyyy, HH:mm")
                    sb.appendLine("$title: $date")
                }

                is Map<*, *> -> {
                    val formattedValue = formatMapToString(
                        value as? Map<String, Any>,
                        "\t\t$indentation",
                        " - ",
                        level + 1
                    )
                    sb.append("$title:\n$formattedValue${if (level < 2) "\n" else ""}")
                }

                else -> {
                    sb.appendLine("$title: $value")
                }
            }
        }

        val statusString = sb.trimEnd()

        return if (statusString.isEmpty()) "" else "$statusString\n"
    }

}
