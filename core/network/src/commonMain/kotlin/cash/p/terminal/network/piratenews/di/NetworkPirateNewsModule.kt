package cash.p.terminal.network.piratenews.di

import cash.p.terminal.network.piratenews.data.repository.PirateNewsRepositoryImpl
import cash.p.terminal.network.pirate.domain.useCase.GetChangeNowAssociatedCoinTickerUseCase
import cash.p.terminal.network.piratenews.api.PirateNewsApi
import cash.p.terminal.network.piratenews.data.mapper.PirateNewsMapper
import cash.p.terminal.network.piratenews.domain.repository.PirateNewsRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val networkPirateNewsModule = module {
    factoryOf(::PirateNewsApi)
    factoryOf(::PirateNewsRepositoryImpl) bind PirateNewsRepository::class
    factoryOf(::PirateNewsMapper)
    factoryOf(::GetChangeNowAssociatedCoinTickerUseCase)
}