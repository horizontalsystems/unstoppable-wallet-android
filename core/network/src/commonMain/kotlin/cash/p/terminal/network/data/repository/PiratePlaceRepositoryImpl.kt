package cash.p.terminal.network.data.repository

import cash.p.terminal.network.api.PlaceApi
import cash.p.terminal.network.data.mapper.PiratePlaceMapper
import cash.p.terminal.network.domain.repository.PiratePlaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PiratePlaceRepositoryImpl(
    private val placeApi: PlaceApi,
    private val piratePlaceMapper: PiratePlaceMapper
) : PiratePlaceRepository {
    override suspend fun getInvestmentData(coin: String, address: String) = withContext(Dispatchers.IO) {
        placeApi.getInvestmentData(coin = coin, address = address).let(piratePlaceMapper::mapInvestmentData)
    }

    override suspend fun getInvestmentChart(coin: String, address: String, period: String) =
        withContext(Dispatchers.IO) {
            placeApi.getInvestmentChart(coin = coin, address = address, period = period)
                .let(piratePlaceMapper::mapInvestmentGraphData)
        }

    override suspend fun getStakeData(coin: String, address: String) = withContext(Dispatchers.IO) {
        placeApi.getStakeData(coin = coin, address = address).let(piratePlaceMapper::mapStakeData)
    }

    override suspend fun getCalculatorData(coin: String, amount: Double) = withContext(Dispatchers.IO) {
        placeApi.getCalculatorData(coin, amount).let(piratePlaceMapper::mapCalculatorData)
    }
}