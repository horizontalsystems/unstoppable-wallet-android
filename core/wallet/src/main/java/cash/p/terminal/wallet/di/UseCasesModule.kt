package cash.p.terminal.wallet.di

import cash.p.terminal.wallet.useCases.RemoveMoneroWalletFilesUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val useCasesModule = module {
    factoryOf(::RemoveMoneroWalletFilesUseCase)
}