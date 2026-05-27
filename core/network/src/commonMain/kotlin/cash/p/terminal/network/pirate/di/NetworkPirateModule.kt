package cash.p.terminal.network.pirate.di

import cash.p.terminal.network.pirate.api.PirateApi
import cash.p.terminal.network.pirate.api.PlaceApi
import cash.p.terminal.network.pirate.domain.repository.PiratePlaceRepository
import cash.p.terminal.network.pirate.data.mapper.MasterNodesMapper
import cash.p.terminal.network.pirate.data.mapper.PiratePlaceMapper
import cash.p.terminal.network.pirate.data.repository.MasterNodesRepositoryImpl
import cash.p.terminal.network.pirate.data.repository.PiratePlaceRepositoryImpl
import cash.p.terminal.network.pirate.domain.repository.MasterNodesRepository
import cash.p.terminal.network.pirate.domain.useCase.GetChangeNowAssociatedCoinTickerUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

const val PREMIUM_API_BASE_URL_QUALIFIER = "premiumApiBaseUrl"

val networkPirateModule = module {
    factoryOf(::PirateApi)
    single {
        PlaceApi(
            httpClient = get(),
            appHeadersProvider = get(),
            premiumApiBaseUrl = get(named(PREMIUM_API_BASE_URL_QUALIFIER)),
        )
    }
    factoryOf(::MasterNodesRepositoryImpl) bind MasterNodesRepository::class
    factoryOf(::PiratePlaceRepositoryImpl) bind PiratePlaceRepository::class
    factoryOf(::MasterNodesMapper)
    factoryOf(::PiratePlaceMapper)
    factoryOf(::GetChangeNowAssociatedCoinTickerUseCase)
}
