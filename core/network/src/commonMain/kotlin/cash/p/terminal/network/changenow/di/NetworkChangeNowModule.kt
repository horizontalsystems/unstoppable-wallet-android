package cash.p.terminal.network.changenow.di

import cash.p.terminal.network.changenow.api.ChangeNowApi
import cash.p.terminal.network.changenow.data.mapper.ChangeNowMapper
import cash.p.terminal.network.changenow.data.repository.ChangeNowRepositoryImpl
import cash.p.terminal.network.changenow.domain.repository.ChangeNowRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val networkChangeNowModule = module {
    factoryOf(::ChangeNowApi)
    factoryOf(::ChangeNowRepositoryImpl) bind ChangeNowRepository::class
    factoryOf(::ChangeNowMapper)
}