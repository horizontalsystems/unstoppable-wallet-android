package cash.p.terminal.wallet.di

import cash.p.terminal.wallet.providers.mapper.PirateCoinInfoMapper
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val mappersModule = module {
    factoryOf(::PirateCoinInfoMapper)
}