package cash.p.terminal.wallet.storage

import cash.p.terminal.wallet.models.GlobalMarketInfo
import io.horizontalsystems.core.models.HsTimePeriod

class GlobalMarketInfoStorage(marketDatabase: MarketDatabase) {
    private val dao = marketDatabase.globalMarketInfoDao()

    fun globalMarketInfo(currencyCode: String, timePeriod: HsTimePeriod): GlobalMarketInfo? {
        return dao.getGlobalMarketInfo(currencyCode, timePeriod)
    }

    fun save(globalMarketInfo: GlobalMarketInfo) {
        dao.insert(globalMarketInfo)
    }
}
