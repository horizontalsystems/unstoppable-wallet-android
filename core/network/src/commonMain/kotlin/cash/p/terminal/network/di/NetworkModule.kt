package cash.p.terminal.network.di

import cash.p.terminal.network.api.PlaceApi
import cash.p.terminal.network.data.buildNetworkClient
import cash.p.terminal.network.data.mapper.PiratePlaceMapper
import cash.p.terminal.network.data.repository.PiratePlaceRepositoryImpl
import cash.p.terminal.network.domain.repository.PiratePlaceRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {
    single { buildNetworkClient() }
    factoryOf(::PlaceApi)
    factoryOf(::PiratePlaceRepositoryImpl) bind PiratePlaceRepository::class
    factoryOf(::PiratePlaceMapper)
}