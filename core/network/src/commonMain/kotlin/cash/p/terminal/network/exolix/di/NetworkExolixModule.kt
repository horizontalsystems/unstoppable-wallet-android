package cash.p.terminal.network.exolix.di

import cash.p.terminal.network.exolix.api.ExolixApi
import cash.p.terminal.network.exolix.data.mapper.ExolixMapper
import cash.p.terminal.network.exolix.data.repository.ExolixRepositoryImpl
import cash.p.terminal.network.exolix.domain.repository.ExolixRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val networkExolixModule = module {
    singleOf(::ExolixApi)
    singleOf(::ExolixRepositoryImpl) bind ExolixRepository::class
    singleOf(::ExolixMapper)
}
