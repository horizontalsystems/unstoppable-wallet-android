package cash.p.terminal.di

import cash.p.terminal.data.repository.EvmTransactionRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val repositoryModule = module {
    factoryOf(::EvmTransactionRepository)
}