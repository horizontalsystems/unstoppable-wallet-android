package io.horizontalsystems.marketkit.storage

import io.horizontalsystems.marketkit.models.GlobalMarketInfo
import io.horizontalsystems.marketkit.models.HsTimePeriod

class GlobalMarketInfoStorage(marketDatabase: MarketDatabase) {
    private val dao = marketDatabase.globalMarketInfoDao()

    fun globalMarketInfo(currencyCode: String, timePeriod: HsTimePeriod): GlobalMarketInfo? {
        return dao.getGlobalMarketInfo(currencyCode, timePeriod)
    }

    fun save(globalMarketInfo: GlobalMarketInfo) {
        dao.insert(globalMarketInfo)
    }
}
