package io.horizontalsystems.bankwallet.modules.settings.main

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule

object MainSettingsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = MainSettingsService(
                App.localStorage,
                App.backupManager,
                App.languageManager,
                App.systemInfoManager,
                App.currencyManager,
                App.termsManager,
                App.pinComponent,
                App.walletConnectSessionManager
            )
            val viewModel = MainSettingsViewModel(
                service,
                App.appConfigProvider.companyWebPageLink,
            )

            return viewModel as T
        }
    }

}

class SettingViewItem(
    val setting: AppSetting,
    val value: String? = null,
    val showAlert: Boolean = false
)

enum class AppSetting {
    ManageWallets,
    SecurityCenter,
    WalletConnect,
    LaunchScreen,
    BaseCurrency,
    Language,
    Theme,
    Experimental,
    FAQ,
    Academy,
    AboutApp,
    WhatsNew,
    AppStatus,
    Terms,
    Github,
    AppWebsite,
    RateUs,
    TellFriends,
    Contact;

    val navigationBundle: Bundle?
        get() = when (this) {
            ManageWallets -> bundleOf(ManageAccountsModule.MODE to ManageAccountsModule.Mode.Manage)
            else -> null
        }

    val title: Int
        get() = when (this) {
            ManageWallets -> R.string.SettingsSecurity_ManageKeys
            SecurityCenter -> R.string.Settings_SecurityCenter
            WalletConnect -> R.string.Settings_WalletConnect
            LaunchScreen -> R.string.Settings_LaunchScreen
            BaseCurrency -> R.string.Settings_BaseCurrency
            Language -> R.string.Settings_Language
            Theme -> R.string.Settings_Theme
            Experimental -> R.string.Settings_ExperimentalFeatures
            FAQ -> R.string.Settings_Faq
            Academy -> R.string.Guides_Title
            AboutApp -> R.string.SettingsAboutApp_Title
            WhatsNew -> R.string.SettingsAboutApp_WhatsNew
            AppStatus -> R.string.Settings_AppStatus
            Terms -> R.string.Settings_Terms
            Github -> R.string.SettingsAboutApp_Github
            AppWebsite -> R.string.SettingsAboutApp_Site
            RateUs -> R.string.Settings_RateUs
            TellFriends -> R.string.Settings_ShareThisWallet
            Contact -> R.string.SettingsContact_Title
        }

    val icon: Int
        get() = when (this) {
            ManageWallets -> R.drawable.ic_wallet_20
            SecurityCenter -> R.drawable.ic_security
            WalletConnect -> R.drawable.ic_wallet_connect_20
            LaunchScreen -> R.drawable.ic_screen_20
            BaseCurrency -> R.drawable.ic_currency
            Language -> R.drawable.ic_language
            Theme -> R.drawable.ic_light_mode
            Experimental -> R.drawable.ic_experimental
            FAQ -> R.drawable.ic_faq_20
            Academy -> R.drawable.ic_academy_20
            AboutApp -> R.drawable.ic_about_app_20
            WhatsNew -> R.drawable.ic_info_20
            AppStatus -> R.drawable.ic_app_status
            Terms -> R.drawable.ic_terms_20
            Github -> R.drawable.ic_github_20
            AppWebsite -> R.drawable.ic_globe
            RateUs -> R.drawable.ic_star_20
            TellFriends -> R.drawable.ic_share_20
            Contact -> R.drawable.ic_email
        }

    val destination: Int?
        get() = when (this) {
            ManageWallets -> R.id.mainFragment_to_manageKeysFragment
            SecurityCenter -> R.id.mainFragment_to_securitySettingsFragment
            WalletConnect -> R.id.mainFragment_to_walletConnect
            LaunchScreen -> R.id.launchScreenSettingsFragment
            BaseCurrency -> R.id.mainFragment_to_baseCurrencySettingsFragment
            Language -> R.id.mainFragment_to_languageSettingsFragment
            Theme -> R.id.mainFragment_to_themeSwitchFragment
            Experimental -> R.id.mainFragment_to_experimentalFeaturesFragment
            FAQ -> R.id.mainFragment_to_faqListFragment
            Academy -> R.id.mainFragment_to_academyFragment
            AboutApp -> R.id.mainFragment_to_aboutAppFragment
            WhatsNew -> R.id.aboutAppFragment_to_releaseNotesFragment
            AppStatus -> R.id.aboutAppFragment_to_appStatusFragment
            Terms -> R.id.aboutAppFragment_to_termsFragment
            else -> null
        }

}
