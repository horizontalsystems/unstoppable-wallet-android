package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import io.horizontalsystems.bankwallet.core.App

class MigrateFavoriteCoinIdsWorker(ctx: Context, params: WorkerParameters) :
    Worker(ctx, params) {

    private val nonStandardCoinIds = mapOf(
        "binanceSmartChain" to "binancecoin",
        "bitcoinCash" to "bitcoin-cash"
    )

    override fun doWork(): Result {
        val localStorage = App.localStorage
        val coinManager = App.coinManager
        val favoritesManager = App.marketFavoritesManager

        val coinUidsToRemove = mutableListOf<String>()
        val coinUidsToAdd = mutableListOf<String>()

        val favoriteCoins = favoritesManager.getAll()

        //handle uid like "binanceSmartChain"
        favoriteCoins.forEach { coin ->
            nonStandardCoinIds[coin.coinUid]?.let {
                coinUidsToRemove.add(coin.coinUid)
                coinUidsToAdd.add(it)
            }
        }

        val favoritesToUpdate = favoriteCoins.filter { it.coinUid.contains("|") }
        coinUidsToRemove.addAll(favoritesToUpdate.map { it.coinUid })

        val coinTypeIds = favoritesToUpdate.map { it.coinUid }

        //handle uid like "erc20|0x6de037ef9ad2725eb40118bb1702ebb27e4aeb24"
        coinManager.getPlatformCoinsByCoinTypeIds(coinTypeIds).let { platformCoins ->
            coinUidsToAdd.addAll(platformCoins.map { it.coin.uid })
        }

        //handle uid like "unsupported|ripple"
        favoritesToUpdate
            .filter { it.coinUid.contains("unsupported") }
            .forEach { coin ->
                val chunked = coin.coinUid.split("|")
                if (chunked.size == 2) {
                    val oldUid = chunked[1]
                    coinUidsToAdd.add(oldUid)
                }
            }

        coinUidsToAdd.forEach { coinNewUid ->
            favoritesManager.add(coinNewUid)
        }

        coinUidsToRemove.forEach { coinOldUid ->
            favoritesManager.remove(coinOldUid)
        }

        localStorage.favoriteCoinIdsMigrated = true

        return Result.success()
    }

}
