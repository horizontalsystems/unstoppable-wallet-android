package cash.p.terminal.wallet.managers

import cash.p.terminal.wallet.models.GlobalMarketInfo
import cash.p.terminal.wallet.models.GlobalMarketPoint
import io.horizontalsystems.core.models.HsTimePeriod
import cash.p.terminal.wallet.providers.HsProvider
import cash.p.terminal.wallet.storage.GlobalMarketInfoStorage
import io.reactivex.Single

class GlobalMarketInfoManager(
    private val provider: HsProvider,
    private val storage: GlobalMarketInfoStorage
) {
    private val expirationInterval = 600 // 10 minutes

    fun globalMarketInfoSingle(currencyCode: String, timePeriod: HsTimePeriod): Single<List<GlobalMarketPoint>> {
        val currentTimestamp = System.currentTimeMillis() / 1000

        storage.globalMarketInfo(currencyCode, timePeriod)?.let { data ->
            if (currentTimestamp - data.timestamp <= expirationInterval)
                return Single.just(data.points)
        }

        return provider.getGlobalMarketPointsSingle(currencyCode, timePeriod)
            .map { globalMarketPoints ->
                storage.save(GlobalMarketInfo(currencyCode, timePeriod, globalMarketPoints, currentTimestamp))

                globalMarketPoints
            }
    }
}
