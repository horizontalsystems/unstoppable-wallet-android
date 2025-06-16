package cash.p.terminal.tangem.di

import cash.p.terminal.tangem.domain.sdk.CardSdkConfigRepository
import cash.p.terminal.tangem.domain.sdk.CardSdkProvider
import cash.p.terminal.tangem.domain.sdk.TangemSdkManager
import cash.p.terminal.tangem.domain.usecase.BackupHardwareWalletUseCase
import cash.p.terminal.tangem.domain.usecase.CollectDerivationsUseCase
import cash.p.terminal.tangem.domain.usecase.ResetToFactorySettingsUseCase
import cash.p.terminal.tangem.domain.usecase.SignHashesTransactionUseCase
import cash.p.terminal.tangem.domain.usecase.SignOneHashTransactionUseCase
import cash.p.terminal.tangem.domain.usecase.TangemBlockchainTypeExistUseCase
import cash.p.terminal.tangem.domain.usecase.TangemCreatePublicKeyUseCase
import cash.p.terminal.tangem.domain.usecase.TangemCreateWalletsUseCase
import cash.p.terminal.tangem.domain.usecase.TangemScanUseCase
import cash.p.terminal.tangem.domain.usecase.ValidateBackUseCase
import cash.p.terminal.tangem.ui.HardwareWalletOnboardingViewModel
import cash.p.terminal.tangem.ui.accesscode.AddAccessCodeViewModel
import cash.p.terminal.wallet.useCases.ScanToAddUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val featureTangemModule = module {
    factoryOf(::TangemScanUseCase)
    factoryOf(::TangemScanUseCase) bind ScanToAddUseCase::class
    factoryOf(::TangemBlockchainTypeExistUseCase)
    factoryOf(::TangemCreatePublicKeyUseCase)
    factoryOf(::CollectDerivationsUseCase)
    factoryOf(::SignOneHashTransactionUseCase)
    factoryOf(::SignHashesTransactionUseCase)
    factoryOf(::TangemCreateWalletsUseCase)
    factoryOf(::BackupHardwareWalletUseCase)
    singleOf(::ValidateBackUseCase)
    singleOf(::ResetToFactorySettingsUseCase)

    singleOf(::CardSdkProvider)
    singleOf(::TangemSdkManager)
    singleOf(::CardSdkConfigRepository)

    viewModelOf(::HardwareWalletOnboardingViewModel)
    viewModelOf(::AddAccessCodeViewModel)
}