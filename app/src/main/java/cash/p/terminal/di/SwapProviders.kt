package cash.p.terminal.di

import cash.p.terminal.modules.multiswap.providers.changenow.ChangeNowProvider
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val swapProvidersModule = module {
    factoryOf(::ChangeNowProvider)
}