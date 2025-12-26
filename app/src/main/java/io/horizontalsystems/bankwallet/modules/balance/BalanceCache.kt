package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.storage.EnabledWalletsCacheDao
import io.horizontalsystems.bankwallet.entities.EnabledWalletCache
import io.horizontalsystems.bankwallet.entities.Wallet

class BalanceCache(private val dao: EnabledWalletsCacheDao) {
    private var cacheMap: Map<String, BalanceData>

    init {
        cacheMap = convertToCacheMap(dao.getAll())
    }

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
        return cacheMap[key]
    }

    fun setCache(balancesData: Map<Wallet, BalanceData>) {
        val list = balancesData.map { (wallet, balanceData) ->
            EnabledWalletCache(
                wallet.token.tokenQuery.id,
                wallet.account.id,
                balanceData
            )
        }
        cacheMap = cacheMap + convertToCacheMap(list)

        dao.insertAll(list)
    }

}
