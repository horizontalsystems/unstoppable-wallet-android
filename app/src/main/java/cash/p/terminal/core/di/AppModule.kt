package cash.p.terminal.core.di

import cash.p.terminal.di.managerModule
import cash.p.terminal.di.repositoryModule
import cash.p.terminal.di.storageModule
import cash.p.terminal.di.swapProvidersModule
import cash.p.terminal.di.viewModelModule
import cash.p.terminal.featureStacking.di.featureStackingModule
import cash.p.terminal.network.di.networkModule
import cash.p.terminal.wallet.di.walletFeatureModule
import org.koin.dsl.module

val appModule = module {
    includes(
        storageModule,
        managerModule,
        repositoryModule,
        viewModelModule,
        walletFeatureModule,
        featureStackingModule,
        networkModule,
        swapProvidersModule,
        useCaseModule
    )
}