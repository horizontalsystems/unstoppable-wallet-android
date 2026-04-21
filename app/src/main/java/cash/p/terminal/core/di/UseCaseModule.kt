package cash.p.terminal.core.di

import cash.p.terminal.core.usecase.CheckGooglePlayUpdateUseCase
import cash.p.terminal.core.usecase.CreateHardwareWalletUseCase
import cash.p.terminal.core.usecase.GenerateMoneroWalletUseCase
import cash.p.terminal.core.usecase.GetMoneroWalletFilesNameUseCase
import cash.p.terminal.core.usecase.MoneroWalletUseCase
import cash.p.terminal.core.usecase.FetchSwapQuotesUseCase
import cash.p.terminal.core.usecase.SyncPendingMultiSwapUseCase
import cash.p.terminal.core.usecase.UpdateSwapProviderTransactionsStatusUseCase
import cash.p.terminal.core.usecase.ValidateMoneroHeightUseCase
import cash.p.terminal.core.usecase.ValidateMoneroMnemonicUseCase
import cash.p.terminal.domain.usecase.ClearZCashWalletDataUseCase
import cash.p.terminal.domain.usecase.GetLocalizedAssetUseCase
import cash.p.terminal.domain.usecase.DeleteAllContactsUseCase
import cash.p.terminal.domain.usecase.ResetUseCase
import cash.p.terminal.manager.ITorConnectionStatusUseCase
import cash.p.terminal.modules.pin.SendZecOnDuressUseCase
import cash.p.terminal.modules.tor.TorConnectionStatusUseCase
import cash.p.terminal.tangem.domain.usecase.ICreateHardwareWalletUseCase
import cash.p.terminal.wallet.useCases.IGetMoneroWalletFilesNameUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val useCaseModule = module {
    singleOf(::UpdateSwapProviderTransactionsStatusUseCase)
    singleOf(::SyncPendingMultiSwapUseCase)
    factoryOf(::FetchSwapQuotesUseCase)
    factoryOf(::ValidateMoneroMnemonicUseCase)
    factoryOf(::ValidateMoneroHeightUseCase)
    factoryOf(::GetLocalizedAssetUseCase)
    factoryOf(::CheckGooglePlayUpdateUseCase)
    factoryOf(::MoneroWalletUseCase)
    factoryOf(::GenerateMoneroWalletUseCase)
    factoryOf(::CreateHardwareWalletUseCase) bind ICreateHardwareWalletUseCase::class
    factoryOf(::GetMoneroWalletFilesNameUseCase) bind IGetMoneroWalletFilesNameUseCase::class
    singleOf(::TorConnectionStatusUseCase) bind ITorConnectionStatusUseCase::class
    singleOf(::ClearZCashWalletDataUseCase)
    singleOf(::DeleteAllContactsUseCase)
    singleOf(::ResetUseCase)
    singleOf(::SendZecOnDuressUseCase)
}
