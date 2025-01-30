package cash.p.terminal.network.pirate.data.repository

import android.util.LruCache
import cash.p.terminal.network.pirate.api.PlaceApi
import cash.p.terminal.network.pirate.data.mapper.PiratePlaceMapper
import cash.p.terminal.network.pirate.domain.enity.ChangeNowAssociatedCoin
import cash.p.terminal.network.pirate.domain.repository.PiratePlaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class PiratePlaceRepositoryImpl(
    private val placeApi: PlaceApi,
    private val piratePlaceMapper: PiratePlaceMapper
) : PiratePlaceRepository {
    private val mutex = Mutex()
    private val associationCache = LruCache<String, List<ChangeNowAssociatedCoin>>(10)
    override suspend fun getInvestmentData(coin: String, address: String) =
        withContext(Dispatchers.IO) {
            placeApi.getInvestmentData(coin = coin, address = address)
                .let(piratePlaceMapper::mapInvestmentData)
        }

    override suspend fun getChangeNowCoinAssociation(uid: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            associationCache.get(uid) ?: placeApi.getChangeNowCoinAssociation(uid)
                .let(piratePlaceMapper::mapChangeNowCoinAssociationList)
                .apply { associationCache.put(uid, this) }
        }
    }

    override suspend fun getInvestmentChart(coin: String, address: String, period: String) =
        withContext(Dispatchers.IO) {
            placeApi.getInvestmentChart(coin = coin, address = address, period = period)
                .let(piratePlaceMapper::mapInvestmentGraphData)
        }

    override suspend fun getStakeData(coin: String, address: String) = withContext(Dispatchers.IO) {
        placeApi.getStakeData(coin = coin, address = address).let(piratePlaceMapper::mapStakeData)
    }

    override suspend fun getCalculatorData(coin: String, amount: Double) =
        withContext(Dispatchers.IO) {
            placeApi.getCalculatorData(coin, amount).let(piratePlaceMapper::mapCalculatorData)
        }
}