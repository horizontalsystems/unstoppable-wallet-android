package io.horizontalsystems.walletkit.modules.balance

import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.storage.EnabledWalletsCacheDao
import io.horizontalsystems.walletkit.entities.EnabledWalletCache
import io.horizontalsystems.walletkit.entities.Wallet

class BalanceCache(private val dao: EnabledWalletsCacheDao) {
    // Read on first use, not on construction: the balance screens build this cache on
    // the main thread, and a wallet whose adapter is ready never asks for it.
    private val stored by lazy { convertToCacheMap(dao.getAll()) }

    @Volatile
    private var updated = mapOf<String, BalanceData>()

    private fun convertToCacheMap(list: List<EnabledWalletCache>): Map<String, BalanceData> {
        return list.mapNotNull {
            it.balanceData?.let { balanceData ->
                val key = listOf(it.tokenQueryId, it.accountId).joinToString()

                key to balanceData
            }
        }.toMap()
    }

    fun setCache(wallet: Wallet, balanceData: BalanceData) {
        setCache(mapOf(wallet to balanceData))
    }

    fun getCache(wallet: Wallet): BalanceData? {
        val key = listOf(wallet.token.tokenQuery.id, wallet.account.id).joinToString()
        return updated[key] ?: stored[key]
    }

    @Synchronized
    fun setCache(balancesData: Map<Wallet, BalanceData>) {
        val list = balancesData.map { (wallet, balanceData) ->
            EnabledWalletCache(
                wallet.token.tokenQuery.id,
                wallet.account.id,
                balanceData
            )
        }
        updated = updated + convertToCacheMap(list)

        dao.insertAll(list)
    }

}
