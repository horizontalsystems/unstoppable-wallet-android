package io.horizontalsystems.bankwallet.modules.settings.appstatus

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.adapters.BitcoinBaseAdapter
import io.horizontalsystems.bankwallet.core.managers.BinanceSmartChainKitManager
import io.horizontalsystems.bankwallet.core.managers.EthereumKitManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.ISystemInfoManager
import java.util.*
import kotlin.collections.LinkedHashMap

class AppStatusService(
        private val systemInfoManager: ISystemInfoManager,
        private val localStorage: ILocalStorage,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager,
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager,
        private val coinManager: ICoinManager,
        private val ethereumKitManager: EthereumKitManager,
        private val binanceSmartChainKitManager: BinanceSmartChainKitManager,
        private val binanceKitManager: IBinanceKitManager
) {

    val status: LinkedHashMap<String, Any>
        get() {
            val status = LinkedHashMap<String, Any>()

            status["App Info"] = getAppInfo()
            status["App Log"] = AppLog.getLog()
            status["Version History"] = getVersionHistory()
            status["Wallets Status"] = getWalletsStatus()
            status["Blockchain Status"] = getBlockchainStatus()

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

        for (predefinedAccountType in predefinedAccountTypeManager.allTypes) {
            val account = predefinedAccountTypeManager.account(predefinedAccountType) ?: continue
            val title = App.instance.localizedContext().getString(predefinedAccountType.title)

            wallets[title] = getAccountDetails(account)
        }
        return wallets
    }

    private fun getAccountDetails(account: Account): LinkedHashMap<String, Any> {
        val accountDetails = LinkedHashMap<String, Any>()

        when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                accountDetails["Mnemonic"] = accountType.words.count()
            }
        }
        return accountDetails
    }

    private fun getBlockchainStatus(): Map<String, Any> {
        val blockchainStatus = LinkedHashMap<String, Any>()

        blockchainStatus.putAll(getBitcoinForkStatuses())
        ethereumKitManager.statusInfo?.let { blockchainStatus["Ethereum"] = it }
        binanceSmartChainKitManager.statusInfo?.let { blockchainStatus["Binance Smart Chain"] = it }
        binanceKitManager.statusInfo?.let { blockchainStatus["Binance DEX"] = it }

        return blockchainStatus
    }

    private fun getBitcoinForkStatuses(): Map<String, Any> {
        val bitcoinChainStatus = LinkedHashMap<String, Any>()
        val coinTypesToDisplay = listOf(CoinType.Bitcoin, CoinType.BitcoinCash, CoinType.Dash, CoinType.Litecoin)

        coinTypesToDisplay.forEach { coinType ->
            walletManager.wallets.firstOrNull { it.coin.type == coinType }?.let { wallet ->
                (adapterManager.getAdapterForWallet(wallet) as? BitcoinBaseAdapter)?.let { adapter ->
                    bitcoinChainStatus[wallet.coin.title] = adapter.statusInfo
                }
            }
        }
        return bitcoinChainStatus
    }

}
