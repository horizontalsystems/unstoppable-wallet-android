//package io.horizontalsystems.bankwallet.core.managers
//
//import android.content.Context
//import androidx.work.Worker
//import androidx.work.WorkerParameters
//import io.horizontalsystems.bankwallet.core.App
//import io.horizontalsystems.bankwallet.entities.EnabledWallet
//
//class FillWalletInfoWorker(ctx: Context, params: WorkerParameters) :
//    Worker(ctx, params) {
//
//    override fun doWork(): Result {
//        val localStorage = App.localStorage
//        val marketKit = App.marketKit
//        val walletManager = App.walletManager
//        val storage = App.enabledWalletsStorage
//
//        localStorage.fillWalletInfoDone = true
//
//        val enabledWallets = storage.enabledWallets
//        val nonFilledWallets =
//            enabledWallets.filter { it.coinName == null || it.coinCode == null || it.coinDecimals == null }
//
//        if (nonFilledWallets.isEmpty()) {
//            return Result.success()
//        }
//
//        val coinTypeIds = nonFilledWallets.map { it.tokenQueryId }
//        val platformCoins = marketKit.platformCoinsByCoinTypeIds(coinTypeIds)
//
//        val updatedEnabledWallets = mutableListOf<EnabledWallet>()
//
//        platformCoins.forEach { platformCoin ->
//            enabledWallets.firstOrNull { it.tokenQueryId == platformCoin.coinType.id }
//                ?.let { enabledWallet ->
//                    val updatedEnabledWallet = EnabledWallet(
//                        tokenQueryId = enabledWallet.tokenQueryId,
//                        coinSettingsId = enabledWallet.coinSettingsId,
//                        accountId = enabledWallet.accountId,
//                        walletOrder= enabledWallet.walletOrder,
//                        coinName = platformCoin.name,
//                        coinCode = platformCoin.code,
//                        coinDecimals = platformCoin.decimals
//                    )
//                    updatedEnabledWallets.add(updatedEnabledWallet)
//                }
//        }
//
//        storage.save(updatedEnabledWallets)
//
//        walletManager.loadWallets()
//
//        return Result.success()
//    }
//
//}
