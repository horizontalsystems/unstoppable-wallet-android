package cash.p.terminal.di

import cash.p.terminal.domain.usecase.GetReleaseNotesUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val mainUseCaseModule = module {
    factoryOf(::GetReleaseNotesUseCase)
}