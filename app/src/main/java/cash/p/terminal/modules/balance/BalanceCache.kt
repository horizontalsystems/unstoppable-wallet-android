package cash.p.terminal.modules.balance

import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.core.storage.EnabledWalletsCacheDao
import cash.p.terminal.entities.EnabledWalletCache

class BalanceCache(private val dao: EnabledWalletsCacheDao) {
    private var cacheMap: Map<String, BalanceData>

    init {
        cacheMap = convertToCacheMap(dao.getAll())
    }

    private fun convertToCacheMap(list: List<EnabledWalletCache>): Map<String, BalanceData> {
        return list.map {
            val key = listOf(it.tokenQueryId, it.accountId).joinToString()
            key to BalanceData(it.balance, it.balanceLocked)
        }.toMap()
    }

    fun setCache(wallet: cash.p.terminal.wallet.Wallet, balanceData: BalanceData) {
        setCache(mapOf(wallet to balanceData))
    }

    fun getCache(wallet: cash.p.terminal.wallet.Wallet): BalanceData? {
        val key = listOf(wallet.token.tokenQuery.id, wallet.account.id).joinToString()
        return cacheMap[key]
    }

    fun setCache(balancesData: Map<cash.p.terminal.wallet.Wallet, BalanceData>) {
        val list = balancesData.map { (wallet, balanceData) ->
            EnabledWalletCache(
                wallet.token.tokenQuery.id,
                wallet.account.id,
                balanceData.available,
                balanceData.timeLocked
            )
        }
        cacheMap = cacheMap + convertToCacheMap(list)

        dao.insertAll(list)
    }

}
