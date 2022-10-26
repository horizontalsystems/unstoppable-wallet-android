package io.horizontalsystems.bankwallet.modules.settings.appstatus

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.adapters.BitcoinBaseAdapter
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.marketkit.models.BlockchainType
import java.util.*

class AppStatusService(
        private val systemInfoManager: ISystemInfoManager,
        private val localStorage: ILocalStorage,
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager,
        private val marketKit: MarketKitWrapper
) {

    val status: LinkedHashMap<String, Any>
        get() {
            val status = LinkedHashMap<String, Any>()

            status["App Info"] = getAppInfo()
            status["App Log"] = AppLog.getLog()
            status["Version History"] = getVersionHistory()
            status["Wallets Status"] = getWalletsStatus()
            status["Blockchain Status"] = getBlockchainStatus()
            status["Market Last Sync Timestamps"] = getMarketLastSyncTimestamps()

            return status
        }

    private fun getAppInfo(): Map<String, Any> {
        val appInfo = LinkedHashMap<String, Any>()
        appInfo["Current Time"] = Date()
        appInfo["App Version"] = systemInfoManager.appVersion
        appInfo["Device Model"] = systemInfoManager.deviceModel
        appInfo["OS Version"] = systemInfoManager.osVersion

        return appInfo
    }

    private fun getVersionHistory(): Map<String, Any> {
        val versions = LinkedHashMap<String, Date>()

        localStorage.appVersions.sortedBy { it.timestamp }.forEach { version ->
            versions[version.version] = Date(version.timestamp)
        }
        return versions
    }

    private fun getWalletsStatus(): Map<String, Any> {
        val wallets = LinkedHashMap<String, Any>()

        for (account in accountManager.accounts) {
            val title = account.name

            wallets[title] = getAccountDetails(account)
        }
        return wallets
    }

    private fun getAccountDetails(account: Account): LinkedHashMap<String, Any> {
        val accountDetails = LinkedHashMap<String, Any>()

        accountDetails["Origin"] = account.origin.value

        val accountType = account.type
        if (accountType is AccountType.Mnemonic) {
            accountDetails["Mnemonic"] = accountType.words.count()
        }
        return accountDetails
    }

    private fun getBlockchainStatus(): Map<String, Any> {
        val blockchainStatus = LinkedHashMap<String, Any>()

        blockchainStatus.putAll(getBitcoinForkStatuses())

        return blockchainStatus
    }

    private fun getBitcoinForkStatuses(): Map<String, Any> {
        val bitcoinChainStatus = LinkedHashMap<String, Any>()
        val blockchainTypesToDisplay = listOf(BlockchainType.Bitcoin, BlockchainType.BitcoinCash, BlockchainType.Dash, BlockchainType.Litecoin)

        walletManager.activeWallets
                .filter { blockchainTypesToDisplay.contains(it.token.blockchainType) }
                .sortedBy { it.token.coin.name }
                .forEach { wallet ->
                    (adapterManager.getAdapterForWallet(wallet) as? BitcoinBaseAdapter)?.let { adapter ->
                        val settings = wallet.configuredToken.coinSettings
                        val settingsValue = settings.derivation?.value
                                ?: settings.bitcoinCashCoinType?.value
                        val statusTitle = "${wallet.token.coin.name}${settingsValue?.let { "-$it" } ?: ""}"
                        bitcoinChainStatus[statusTitle] = adapter.statusInfo
                    }
                }
        return bitcoinChainStatus
    }

    private fun getMarketLastSyncTimestamps(): Map<String, Any> {
        val syncInfo = marketKit.syncInfo()
        val info = LinkedHashMap<String, Any>()
        info["Coins"] = syncInfo.coinsTimestamp ?: ""
        info["Blockchains"] = syncInfo.blockchainsTimestamp ?: ""
        info["Tokens"] = syncInfo.tokensTimestamp ?: ""

        return info
    }

}
