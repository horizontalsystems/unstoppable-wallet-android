package cash.p.terminal.network.di

import SolanaRpcApiImpl
import cash.p.terminal.network.binance.api.BinanceApi
import cash.p.terminal.network.binance.api.BinanceApiImpl
import cash.p.terminal.network.binance.api.EthereumRpcApi
import cash.p.terminal.network.binance.api.EthereumRpcApiImpl
import cash.p.terminal.network.binance.api.SolanaRpcApi
import cash.p.terminal.network.binance.api.TonRpcApi
import cash.p.terminal.network.binance.api.TonRpcApiImpl
import cash.p.terminal.network.binance.api.TronRpcApi
import cash.p.terminal.network.binance.api.TronRpcApiImpl
import cash.p.terminal.network.changenow.data.repository.ChangeNowRepositoryImpl
import cash.p.terminal.network.changenow.di.networkChangeNowModule
import cash.p.terminal.network.data.buildNetworkClient
import cash.p.terminal.network.exolix.data.repository.ExolixRepositoryImpl
import cash.p.terminal.network.exolix.di.networkExolixModule
import cash.p.terminal.network.pirate.di.networkPirateModule
import cash.p.terminal.network.piratenews.di.networkPirateNewsModule
import cash.p.terminal.network.quickex.data.repository.QuickexRepositoryImpl
import cash.p.terminal.network.quickex.di.networkQuickexModule
import cash.p.terminal.network.stonfi.di.networkStonFiModule
import cash.p.terminal.network.swaprepository.SwapProvider
import cash.p.terminal.network.swaprepository.SwapProviderTransactionStatusRepository
import cash.p.terminal.network.zcash.di.networkZcashModule
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {
    single { buildNetworkClient() }

    // API
    factoryOf(::EthereumRpcApiImpl) bind EthereumRpcApi::class
    factoryOf(::BinanceApiImpl) bind BinanceApi::class
    factoryOf(::SolanaRpcApiImpl) bind SolanaRpcApi::class
    factoryOf(::TronRpcApiImpl) bind TronRpcApi::class
    factoryOf(::TonRpcApiImpl) bind TonRpcApi::class

    single<SwapProviderTransactionStatusRepository>(named(SwapProvider.CHANGENOW)) {
        get<ChangeNowRepositoryImpl>()
    }
    single<SwapProviderTransactionStatusRepository>(named(SwapProvider.QUICKEX)) {
        get<QuickexRepositoryImpl>()
    }
    single<SwapProviderTransactionStatusRepository>(named(SwapProvider.EXOLIX)) {
        get<ExolixRepositoryImpl>()
    }

    includes(
        networkPirateModule,
        networkChangeNowModule,
        networkQuickexModule,
        networkExolixModule,
        networkPirateNewsModule,
        networkStonFiModule,
        networkZcashModule,
        databaseModule,
        decoderModule
    )
}
