package cash.p.terminal.di

import cash.p.terminal.modules.hardwarewallet.HardwareWalletViewModel
import cash.p.terminal.modules.resettofactorysettings.ResetToFactorySettingsViewModel
import cash.p.terminal.modules.settings.displaytransactions.DisplayTransactionsViewModel
import cash.p.terminal.modules.settings.privacy.PrivacyViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::DisplayTransactionsViewModel)
    viewModelOf(::PrivacyViewModel)
    viewModelOf(::HardwareWalletViewModel)
    viewModelOf(::ResetToFactorySettingsViewModel)
}
