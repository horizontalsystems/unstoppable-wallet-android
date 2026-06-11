package cash.p.terminal.network.changenow.di

import cash.p.terminal.network.changenow.api.ChangeNowApi
import cash.p.terminal.network.changenow.data.mapper.ChangeNowMapper
import cash.p.terminal.network.changenow.data.repository.ChangeNowRepositoryImpl
import cash.p.terminal.network.changenow.domain.repository.ChangeNowRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val networkChangeNowModule = module {
    singleOf(::ChangeNowApi)
    singleOf(::ChangeNowRepositoryImpl) bind ChangeNowRepository::class
    singleOf(::ChangeNowMapper)
}