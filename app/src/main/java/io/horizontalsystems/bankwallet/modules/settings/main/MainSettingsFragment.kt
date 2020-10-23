package io.horizontalsystems.bankwallet.modules.settings.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.core.*
import io.horizontalsystems.languageswitcher.LanguageSettingsFragment
import kotlinx.android.synthetic.main.fragment_settings.*

class MainSettingsFragment : BaseFragment() {

    private val presenter by viewModels<MainSettingsPresenter> { MainSettingsModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViewListeners()

        subscribeToViewEvents(presenter.view as MainSettingsView)
        subscribeToRouterEvents(presenter.router as MainSettingsRouter)
        subscribeFragmentResult()

        presenter.viewDidLoad()
    }

    private fun bindViewListeners() {
        manageKeys.setOnSingleClickListener { presenter.didTapManageKeys() }

        privacySettings.setOnSingleClickListener { presenter.didTapSecurity() }

        notifications.setOnSingleClickListener { presenter.didTapNotifications() }

        appStatus.setOnSingleClickListener { presenter.didTapAppStatus() }

        baseCurrency.setOnSingleClickListener { presenter.didTapBaseCurrency() }

        language.setOnSingleClickListener { presenter.didTapLanguage() }

        lightMode.setOnSingleClickListener { lightMode.switchToggle() }

        lightMode.setOnCheckedChangeListener {
            presenter.didSwitchLightMode(it)
        }

        experimentalFeatures.setOnSingleClickListener { presenter.didTapExperimentalFeatures() }

        terms.setOnSingleClickListener { presenter.didTapAbout() }

        contact.setOnSingleClickListener { presenter.didTapReportProblem() }

        shareApp.setOnSingleClickListener { presenter.didTapTellFriends() }

        companyLogo.setOnSingleClickListener { presenter.didTapCompanyLogo() }
    }

    private fun subscribeToViewEvents(presenterView: MainSettingsView) {
        presenterView.baseCurrency.observe(viewLifecycleOwner, Observer { currency ->
            baseCurrency.showValue(currency)
        })

        presenterView.backedUp.observe(viewLifecycleOwner, Observer { wordListBackedUp ->
            manageKeys.showAttention(!wordListBackedUp)
        })

        presenterView.pinSet.observe(viewLifecycleOwner, Observer { pinSet ->
            privacySettings.showAttention(!pinSet)
        })

        presenterView.language.observe(viewLifecycleOwner, Observer { languageCode ->
            language.showValue(languageCode)
        })

        presenterView.lightMode.observe(viewLifecycleOwner, Observer {
            lightMode.setChecked(it)
        })

        presenterView.appVersion.observe(viewLifecycleOwner, Observer { version ->
            version?.let {
                var appVersion = getString(R.string.Settings_InfoTitleWithVersion, it)
                if (getString(R.string.is_release) == "false") {
                    appVersion = "$appVersion (${BuildConfig.VERSION_CODE})"
                }
                appName.text = appVersion
            }
        })

        presenterView.termsAccepted.observe(viewLifecycleOwner, Observer { termsAccepted ->
            terms.showAttention(!termsAccepted)
        })
    }

    private fun subscribeToRouterEvents(router: MainSettingsRouter) {
        router.showManageKeysLiveEvent.observe(this, Observer {
            findNavController().navigate(R.id.mainFragment_to_manageKeysFragment, null, navOptions())
        })

        router.showBaseCurrencySettingsLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().navigate(R.id.mainFragment_to_currencySwitcherFragment, null, navOptions())
        })

        router.showLanguageSettingsLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().navigate(R.id.mainFragment_to_languageSettingsFragment, null, navOptions())
        })

        router.showAboutLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().navigate(R.id.mainFragment_to_termsFragment, null, navOptions())
        })

        router.showNotificationsLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().navigate(R.id.mainFragment_to_notificationsFragment, null, navOptions())
        })

        router.showReportProblemLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().navigate(R.id.mainFragment_to_contactFragment, null, navOptions())
        })

        router.showSecuritySettingsLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().navigate(R.id.mainFragment_to_securitySettingsFragment, null, navOptions())
        })

        router.showExperimentalFeaturesLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().navigate(R.id.mainFragment_to_experimentalFeaturesFragment, null, navOptions())
        })

        router.openLinkLiveEvent.observe(viewLifecycleOwner, Observer { link ->
            val uri = Uri.parse(link)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            activity?.startActivity(intent)
        })

        router.shareAppLiveEvent.observe(viewLifecycleOwner, Observer { appWebPageLink ->
            val shareMessage = getString(R.string.SettingsShare_Text) + "\n" + appWebPageLink + "\n"
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.SettingsShare_Title)))
        })

        router.reloadAppLiveEvent.observe(viewLifecycleOwner, Observer {
            val nightMode = if (CoreApp.themeStorage.isLightModeOn)
                AppCompatDelegate.MODE_NIGHT_NO else
                AppCompatDelegate.MODE_NIGHT_YES

            AppCompatDelegate.setDefaultNightMode(nightMode)
        })

        router.openAppStatusLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().navigate(R.id.mainFragment_to_appStatusFragment, null, navOptions())
        })
    }

    private fun subscribeFragmentResult() {
        getNavigationLiveData(LanguageSettingsFragment.LANGUAGE_CHANGE)?.observe(viewLifecycleOwner, Observer {
            activity?.let { MainModule.startAsNewTask(it, MainActivity.SETTINGS_TAB_POSITION) }
        })
    }
}
