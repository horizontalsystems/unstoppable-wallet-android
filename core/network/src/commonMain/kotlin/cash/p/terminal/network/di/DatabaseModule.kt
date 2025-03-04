package cash.p.terminal.network.di

import cash.p.terminal.network.pirate.data.database.CacheAppDatabase
import org.koin.dsl.module

internal val databaseModule = module {
    single { CacheAppDatabase.buildDatabase(get()) }
    single { get<CacheAppDatabase>().changeNowCoinDao() }
}
