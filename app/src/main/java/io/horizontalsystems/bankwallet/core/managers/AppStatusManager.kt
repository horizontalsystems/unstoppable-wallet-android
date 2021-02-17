package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.adapters.BitcoinBaseAdapter
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.core.ISystemInfoManager
import java.util.*
import kotlin.collections.LinkedHashMap

class AppStatusManager(
        private val systemInfoManager: ISystemInfoManager,
        private val localStorage: ILocalStorage,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager,
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager,
        private val coinManager: ICoinManager,
        private val ethereumKitManager: EthereumKitManager,
        private val binanceSmartChainKitManager: BinanceSmartChainKitManager,
        private val binanceKitManager: IBinanceKitManager
) : IAppStatusManager {

    override val status: LinkedHashMap<String, Any>
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
            val title = App.instance.getString(predefinedAccountType.title)

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
        val coinIdsToDisplay = listOf("BTC", "BCH", "DASH", "LTC")

        for (coinId in coinIdsToDisplay) {
            val coin = getCoin(coinId)
            val wallet = walletManager.wallet(coin) ?: continue
            val adapter = adapterManager.getAdapterForWallet(wallet) as? BitcoinBaseAdapter
                    ?: continue
            bitcoinChainStatus[coin.title] = adapter.statusInfo
        }
        return bitcoinChainStatus
    }

    private fun getCoin(coinId: String): Coin {
        return coinManager.coins.first { it.coinId == coinId }
    }

}
