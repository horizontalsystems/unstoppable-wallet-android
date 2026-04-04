package cash.p.terminal.di

import cash.p.terminal.modules.balance.token.addresspoisoning.AddressPoisoningViewModel
import cash.p.terminal.modules.blockchainstatus.BlockchainStatusViewModel
import cash.p.terminal.modules.configuredtoken.ConfiguredTokenInfoViewModel
import cash.p.terminal.modules.addtoken.AddTokenViewModel
import cash.p.terminal.modules.createaccount.CreateAdvancedAccountViewModel
import cash.p.terminal.modules.createaccount.passphraseterms.PassphraseTermsViewModel
import cash.p.terminal.modules.manageaccount.safetyrules.SafetyRulesModule
import cash.p.terminal.modules.manageaccount.safetyrules.SafetyRulesViewModel
import cash.p.terminal.modules.multiswap.TimerService
import cash.p.terminal.modules.multiswap.exchange.MultiSwapExchangeViewModel
import cash.p.terminal.modules.multiswap.exchanges.MultiSwapExchangesViewModel
import cash.p.terminal.modules.displayoptions.DisplayOptionsViewModel
import cash.p.terminal.modules.hardwarewallet.HardwareWalletViewModel
import cash.p.terminal.modules.importwallet.ImportWalletViewModel
import cash.p.terminal.modules.main.MainActivityViewModel
import cash.p.terminal.modules.market.favorites.MarketFavoritesViewModel
import cash.p.terminal.modules.manageaccount.backupkey.BackupKeyViewModel
import cash.p.terminal.modules.main.MainViewModel
import cash.p.terminal.modules.pin.unlock.PinUnlockViewModel
import cash.p.terminal.modules.moneroconfigure.MoneroConfigureViewModel
import cash.p.terminal.modules.premium.about.AboutPremiumViewModel
import cash.p.terminal.modules.premium.settings.PremiumSettingsViewModel
import cash.p.terminal.modules.premium.smsnotification.SendSmsNotificationViewModel
import cash.p.terminal.modules.receive.viewmodels.ReceiveMoneroViewModel
import cash.p.terminal.modules.qrscanner.QRScannerViewModel
import cash.p.terminal.modules.qrscanner.QrCodeImageDecoder
import cash.p.terminal.modules.releasenotes.ReleaseNotesViewModel
import cash.p.terminal.modules.resettofactorysettings.ResetToFactorySettingsViewModel
import cash.p.terminal.modules.restoreaccount.duplicatewallet.DuplicateWalletViewModel
import cash.p.terminal.modules.restoreaccount.restoremnemonic.RestoreMnemonicViewModel
import cash.p.terminal.modules.settings.advancedsecurity.AdvancedSecurityViewModel
import cash.p.terminal.modules.settings.advancedsecurity.securereset.SecureResetTermsViewModel
import cash.p.terminal.modules.settings.advancedsecurity.terms.HiddenWalletTermsViewModel
import cash.p.terminal.modules.settings.appcache.AppCacheViewModel
import cash.p.terminal.modules.settings.appstatus.AppStatusViewModel
import cash.p.terminal.modules.settings.displaytransactions.DisplayTransactionsViewModel
import cash.p.terminal.modules.settings.main.MainSettingsViewModel
import cash.p.terminal.modules.settings.privacy.PrivacyViewModel
import cash.p.terminal.modules.settings.security.passcode.SecuritySettingsViewModel
import cash.p.terminal.modules.tonconnect.TonConnectListViewModel
import cash.p.terminal.modules.walletconnect.AccountTypeNotSupportedDialog
import cash.p.terminal.modules.walletconnect.AccountTypeNotSupportedViewModel
import cash.p.terminal.modules.solananetwork.SolanaNetworkService
import cash.p.terminal.modules.solananetwork.SolanaNetworkViewModel
import cash.p.terminal.modules.zcashconfigure.ZcashConfigureViewModel
import cash.p.terminal.modules.multiswap.SwapSelectCoinViewModel
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.DefaultDispatcherProvider
import io.horizontalsystems.core.DispatcherProvider
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val viewModelModule = module {
    singleOf(::DefaultDispatcherProvider) bind DispatcherProvider::class
    singleOf(::QrCodeImageDecoder)

    viewModelOf(::MarketFavoritesViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::MainActivityViewModel)
    viewModelOf(::DisplayTransactionsViewModel)
    viewModelOf(::PrivacyViewModel)
    viewModelOf(::HardwareWalletViewModel)
    viewModelOf(::ImportWalletViewModel)
    viewModelOf(::ResetToFactorySettingsViewModel)
    viewModelOf(::SecuritySettingsViewModel)
    viewModelOf(::MainSettingsViewModel)
    viewModelOf(::ReleaseNotesViewModel)
    viewModelOf(::RestoreMnemonicViewModel)
    viewModelOf(::AppStatusViewModel)
    viewModel { params -> AddressPoisoningViewModel(params[0], params[1], params[2], get(), get(), get(), get()) }
    viewModel { params -> BlockchainStatusViewModel(provider = params.get(), dispatcherProvider = get()) }
    viewModelOf(::AppCacheViewModel)
    viewModelOf(::MoneroConfigureViewModel)
    viewModelOf(::AboutPremiumViewModel)
    viewModelOf(::PremiumSettingsViewModel)
    viewModelOf(::SendSmsNotificationViewModel)
    viewModelOf(::DisplayOptionsViewModel)
    viewModelOf(::TonConnectListViewModel)
    viewModelOf(::ZcashConfigureViewModel)
    factoryOf(::SolanaNetworkService)
    viewModelOf(::SolanaNetworkViewModel)
    viewModelOf(::QRScannerViewModel)
    viewModelOf(::AddTokenViewModel)
    viewModelOf(::PinUnlockViewModel)
    viewModel { (input: AccountTypeNotSupportedDialog.Input) ->
        AccountTypeNotSupportedViewModel(input = input, accountManager = get())
    }
    viewModel { (token: Token) ->
        ConfiguredTokenInfoViewModel(
            token = token,
            accountManager = get(),
            restoreSettingsManager = get()
        )
    }
    viewModel { (accountToCopy: Account) ->
        DuplicateWalletViewModel(
            accountToCopy = accountToCopy,
            accountManager = get(),
            accountFactory = get(),
            moneroWalletUseCase = get(),
            enabledWalletStorage = get(),
            walletManager = get(),
            restoreSettingsManager = get(),
            localStorage = get()
        )
    }
    viewModel { (accountId: String) ->
        BackupKeyViewModel(accountId = accountId, accountManager = get())
    }
    viewModelOf(::AdvancedSecurityViewModel)
    viewModelOf(::HiddenWalletTermsViewModel)
    viewModelOf(::SecureResetTermsViewModel)
    viewModelOf(::CreateAdvancedAccountViewModel)
    viewModel { (termTitles: Array<String>) ->
        PassphraseTermsViewModel(termTitles = termTitles, localStorage = get())
    }
    viewModel { (mode: SafetyRulesModule.SafetyRulesMode, termTitles: List<String>) ->
        SafetyRulesViewModel(mode = mode, termTitles = termTitles, localStorage = get())
    }
    viewModelOf(::MultiSwapExchangesViewModel)
    viewModel { params ->
        MultiSwapExchangeViewModel(
            pendingMultiSwapId = params.get(),
            pendingMultiSwapStorage = get(),
            marketKit = get(),
            numberFormatter = get(),
            onChainMonitor = get(),
            swapQuoteService = get(),
            fetchSwapQuotesUseCase = get(),
            timerService = TimerService(),
            syncPendingMultiSwapUseCase = get(),
            currencyManager = get(),
            adapterManager = get(),
            balanceHiddenManager = get(),
            walletManager = get(),
            walletUseCase = get(),
            accountManager = get(),
        )
    }
    viewModel { (otherSelectedToken: Token?, activeAccount: Account) ->
        SwapSelectCoinViewModel(
            otherSelectedToken = otherSelectedToken,
            activeAccount = activeAccount
        )
    }
    viewModel { (wallet: Wallet) ->
        ReceiveMoneroViewModel(
            wallet = wallet,
            adapterManager = get(),
            localStorage = get(),
            dispatcherProvider = get()
        )
    }
}
