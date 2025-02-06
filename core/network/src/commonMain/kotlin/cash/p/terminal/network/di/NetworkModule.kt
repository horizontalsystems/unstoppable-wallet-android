package cash.p.terminal.network.di

import cash.p.terminal.network.changenow.di.networkChangeNowModule
import cash.p.terminal.network.data.buildNetworkClient
import cash.p.terminal.network.pirate.di.networkPirateModule
import cash.p.terminal.network.piratenews.di.networkPirateNewsModule
import org.koin.dsl.module

val networkModule = module {
    single { buildNetworkClient() }
    includes(
        networkPirateModule,
        networkChangeNowModule,
        networkPirateNewsModule
    )
}