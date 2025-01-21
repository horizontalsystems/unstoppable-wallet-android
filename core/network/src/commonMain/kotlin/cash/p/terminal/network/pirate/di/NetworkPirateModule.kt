package cash.p.terminal.network.pirate.di

import cash.p.terminal.network.pirate.domain.repository.PiratePlaceRepository
import cash.p.terminal.network.pirate.api.PlaceApi
import cash.p.terminal.network.pirate.data.mapper.PiratePlaceMapper
import cash.p.terminal.network.pirate.data.repository.PiratePlaceRepositoryImpl
import cash.p.terminal.network.pirate.domain.useCase.GetChangeNowAssociatedCoinTickerUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val networkPirateModule = module {
    factoryOf(::PlaceApi)
    factoryOf(::PiratePlaceRepositoryImpl) bind PiratePlaceRepository::class
    factoryOf(::PiratePlaceMapper)
    factoryOf(::GetChangeNowAssociatedCoinTickerUseCase)
}