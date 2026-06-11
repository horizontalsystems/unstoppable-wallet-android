package cash.p.terminal.network.quickex.di

import cash.p.terminal.network.quickex.api.QuickexApi
import cash.p.terminal.network.quickex.data.mapper.QuickexMapper
import cash.p.terminal.network.quickex.data.repository.QuickexRepositoryImpl
import cash.p.terminal.network.quickex.domain.repository.QuickexRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val networkQuickexModule = module {
    singleOf(::QuickexApi)
    singleOf(::QuickexRepositoryImpl) bind QuickexRepository::class
    singleOf(::QuickexMapper)
}