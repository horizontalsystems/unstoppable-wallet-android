package cash.p.terminal.tangem.di

import cash.p.terminal.tangem.domain.sdk.CardSdkConfigRepository
import cash.p.terminal.tangem.domain.sdk.CardSdkProvider
import cash.p.terminal.tangem.domain.sdk.TangemSdkManager
import cash.p.terminal.tangem.domain.usecase.CollectDerivationsUseCase
import cash.p.terminal.tangem.domain.usecase.SignHashesTransactionUseCase
import cash.p.terminal.tangem.domain.usecase.SignOneHashTransactionUseCase
import cash.p.terminal.tangem.domain.usecase.TangemBlockchainTypeExistUseCase
import cash.p.terminal.tangem.domain.usecase.TangemCreatePublicKeyUseCase
import cash.p.terminal.tangem.domain.usecase.TangemScanUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val featureTangemModule = module {
    factoryOf(::TangemScanUseCase)
    factoryOf(::TangemBlockchainTypeExistUseCase)
    factoryOf(::TangemCreatePublicKeyUseCase)
    factoryOf(::CollectDerivationsUseCase)
    factoryOf(::SignOneHashTransactionUseCase)
    factoryOf(::SignHashesTransactionUseCase)

    singleOf(::CardSdkProvider)
    singleOf(::TangemSdkManager)
    singleOf(::CardSdkConfigRepository)
}