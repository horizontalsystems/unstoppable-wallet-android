package io.horizontalsystems.bankwallet.modules.settings.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WalletConnectListModule
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.languageswitcher.LanguageSettingsFragment
import io.horizontalsystems.views.ListPosition
import kotlinx.android.synthetic.main.fragment_settings.*

class MainSettingsFragment : BaseFragment() {

    private val presenter by viewModels<MainSettingsPresenter> { MainSettingsModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToRouterEvents(presenter.router as MainSettingsRouter)

        val manageKeys = SettingsMenuItem(R.string.SettingsSecurity_ManageKeys, R.drawable.ic_wallet_20, listPosition = ListPosition.First) {
            presenter.didTapManageKeys()
        }
        val privacySettings = SettingsMenuItem(R.string.Settings_SecurityCenter, R.drawable.ic_security, listPosition = ListPosition.Last) {
            presenter.didTapSecurity()
        }
        val walletConnect = SettingsMenuItem(R.string.Settings_WalletConnect, R.drawable.ic_wallet_connect_20, listPosition = ListPosition.Single) {
            presenter.didTapWalletConnect()
        }
        val launchScreen = SettingsMenuItem(R.string.Settings_LaunchScreen, R.drawable.ic_screen_20, listPosition = ListPosition.First) {
            findNavController().navigate(R.id.launchScreenSettingsFragment, null, navOptions())
        }
        val baseCurrency = SettingsMenuItem(R.string.Settings_BaseCurrency, R.drawable.ic_currency, listPosition = ListPosition.Middle) {
            presenter.didTapBaseCurrency()
        }
        val language = SettingsMenuItem(R.string.Settings_Language, R.drawable.ic_language, listPosition = ListPosition.Middle) {
            presenter.didTapLanguage()
        }
        val theme = SettingsMenuItem(R.string.Settings_Theme, R.drawable.ic_light_mode, listPosition = ListPosition.Middle) {
            presenter.didTapTheme()
        }
        val experimentalFeatures = SettingsMenuItem(R.string.Settings_ExperimentalFeatures, R.drawable.ic_experimental, listPosition = ListPosition.Last) {
            presenter.didTapExperimentalFeatures()
        }
        val faq = SettingsMenuItem(R.string.Settings_Faq, R.drawable.ic_faq_20, listPosition = ListPosition.First) {
            presenter.didTapFaq()
        }
        val academy = SettingsMenuItem(R.string.Guides_Title, R.drawable.ic_academy_20, listPosition = ListPosition.Last) {
            presenter.didTapAcademy()
        }
        val aboutApp = SettingsMenuItem(R.string.SettingsAboutApp_Title, R.drawable.ic_about_app_20, listPosition = ListPosition.Single) {
            presenter.didTapAboutApp()
        }
        val settingsBottom = SettingsMenuBottom {
            presenter.didTapCompanyLogo()
        }

        val presenterView = presenter.view as MainSettingsView
        val mainSettingsAdapter = MainSettingsAdapter(listOf(
                manageKeys,
                privacySettings,
                null,
                walletConnect,
                null,
                launchScreen,
                baseCurrency,
                language,
                theme,
                experimentalFeatures,
                null,
                faq,
                academy,
                null,
                aboutApp,
                settingsBottom
        ))

        settingsRecyclerView.adapter = mainSettingsAdapter
        settingsRecyclerView.setHasFixedSize(true)
        settingsRecyclerView.setItemAnimator(null)

        presenterView.baseCurrency.observe(viewLifecycleOwner, { currency ->
            baseCurrency.value = currency
            mainSettingsAdapter.notifyChanged(baseCurrency)
        })

        presenterView.launchScreen.observe(viewLifecycleOwner, { screen ->
            launchScreen.value = getString(screen.titleRes)
            mainSettingsAdapter.notifyChanged(launchScreen)
        })

        presenterView.backedUp.observe(viewLifecycleOwner, { wordListBackedUp ->
            manageKeys.attention = !wordListBackedUp
            mainSettingsAdapter.notifyChanged(manageKeys)
        })

        presenterView.pinSet.observe(viewLifecycleOwner, { pinSet ->
            privacySettings.attention = !pinSet
            mainSettingsAdapter.notifyChanged(privacySettings)
        })

        presenterView.language.observe(viewLifecycleOwner, { languageCode ->
            language.value = languageCode
            mainSettingsAdapter.notifyChanged(language)
        })

        presenterView.currentThemeName.observe(viewLifecycleOwner, {
            theme.value = getString(it)
            mainSettingsAdapter.notifyChanged(theme)
        })

        presenterView.appVersion.observe(viewLifecycleOwner, { version ->
            var appVersion = getString(R.string.Settings_InfoTitleWithVersion, version)
            if (getString(R.string.is_release) == "false") {
                appVersion += " (${BuildConfig.VERSION_CODE})"
            }

            settingsBottom.appName = appVersion
            mainSettingsAdapter.notifyChanged(settingsBottom)
        })

        presenterView.termsAccepted.observe(viewLifecycleOwner, { termsAccepted ->
            aboutApp.attention = !termsAccepted
            mainSettingsAdapter.notifyChanged(aboutApp)
        })

        presenterView.walletConnectSessionCount.observe(viewLifecycleOwner, { currency ->
            walletConnect.value = currency
            mainSettingsAdapter.notifyChanged(walletConnect)
        })

        presenter.viewDidLoad()
    }

    override fun onResume() {
        super.onResume()
        subscribeFragmentResult()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        settingsRecyclerView.adapter = null
    }

    private fun subscribeToRouterEvents(router: MainSettingsRouter) {
        router.showManageKeysLiveEvent.observe(this, {
            ManageAccountsModule.start(this, R.id.mainFragment_to_manageKeysFragment, navOptions(), ManageAccountsModule.Mode.Manage)
        })

        router.showBaseCurrencySettingsLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.mainFragment_to_baseCurrencySettingsFragment, null, navOptions())
        })

        router.showLanguageSettingsLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.mainFragment_to_languageSettingsFragment, null, navOptions())
        })

        router.showThemeSwitcherLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.mainFragment_to_themeSwitchFragment, null, navOptions())
        })

        router.showAboutLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.mainFragment_to_aboutAppFragment, null, navOptions())
        })

        router.openFaqLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.mainFragment_to_faqListFragment, null, navOptions())
        })

        router.openAcademyLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.mainFragment_to_academyFragment, null, navOptions())
        })

        router.showSecuritySettingsLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.mainFragment_to_securitySettingsFragment, null, navOptions())
        })

        router.showExperimentalFeaturesLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(R.id.mainFragment_to_experimentalFeaturesFragment, null, navOptions())
        })

        router.openLinkLiveEvent.observe(viewLifecycleOwner, { link ->
            context?.let { ctx ->
                LinkHelper.openLinkInAppBrowser(ctx, link)
            }
        })

        router.openWalletConnectLiveEvent.observe(viewLifecycleOwner, {
            WalletConnectListModule.start(this, R.id.mainFragment_to_walletConnect, navOptions())
        })
    }

    private fun subscribeFragmentResult() {
        getNavigationResult(LanguageSettingsFragment.LANGUAGE_CHANGE)?.let {
            presenter.setAppRelaunchingFromSettings()
            activity?.let { MainModule.startAsNewTask(it) }
        }
    }
}
