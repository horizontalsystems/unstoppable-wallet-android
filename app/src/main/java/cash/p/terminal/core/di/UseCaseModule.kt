package cash.p.terminal.core.di

import cash.p.terminal.core.usecase.UpdateChangeNowStatusesUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val useCaseModule = module {
    factoryOf(::UpdateChangeNowStatusesUseCase)
}