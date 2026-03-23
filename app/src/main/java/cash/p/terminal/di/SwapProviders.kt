package cash.p.terminal.di

import cash.p.terminal.modules.multiswap.MultiSwapOnChainMonitor
import cash.p.terminal.modules.multiswap.MultiSwapRouteResolver
import cash.p.terminal.modules.multiswap.SwapQuoteService
import cash.p.terminal.modules.multiswap.providers.ChangeNowProvider
import cash.p.terminal.modules.multiswap.providers.QuickexProvider
import cash.p.terminal.modules.multiswap.providers.StonFiProvider
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val swapProvidersModule = module {
    factoryOf(::SwapQuoteService)

    factoryOf(::ChangeNowProvider)
    factoryOf(::QuickexProvider)
    factoryOf(::StonFiProvider)

    singleOf(::MultiSwapOnChainMonitor)
    singleOf(::MultiSwapRouteResolver)
}
