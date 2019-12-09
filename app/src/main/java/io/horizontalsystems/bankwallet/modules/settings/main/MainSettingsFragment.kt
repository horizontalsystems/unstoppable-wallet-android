package io.horizontalsystems.bankwallet.modules.settings.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.managecoins.ManageWalletsModule
import io.horizontalsystems.bankwallet.modules.notifications.NotificationsModule
import io.horizontalsystems.bankwallet.modules.reportproblem.ReportProblemModule
import io.horizontalsystems.bankwallet.modules.settings.AboutSettingsActivity
import io.horizontalsystems.bankwallet.modules.settings.basecurrency.BaseCurrencySettingsModule
import io.horizontalsystems.bankwallet.modules.settings.experimental.ExperimentalFeaturesModule
import io.horizontalsystems.bankwallet.modules.settings.language.LanguageSettingsModule
import io.horizontalsystems.bankwallet.modules.settings.security.SecuritySettingsModule
import kotlinx.android.synthetic.main.fragment_settings.*

class MainSettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val presenter = ViewModelProviders.of(this, MainSettingsModule.Factory()).get(MainSettingsPresenter::class.java)
        val presenterView = presenter.view as MainSettingsView
        val router = presenter.router as MainSettingsRouter

        bindViewListeners(presenter)

        subscribeToViewEvents(presenterView, presenter)

        subscribeToRouterEvents(router)

        presenter.viewDidLoad()

        //currently language setting is not working on API 23, 24, 25
        language.visibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) View.VISIBLE else View.GONE
    }

    private fun bindViewListeners(presenter: MainSettingsPresenter) {
        securityCenter.setOnClickListener { presenter.didTapSecurity() }

        notifications.setOnClickListener { presenter.didTapNotifications() }

        manageCoins.setOnClickListener { presenter.didManageCoins() }

        experimentalFeatures.setOnClickListener { presenter.didTapExperimentalFeatures() }

        baseCurrency.setOnClickListener { presenter.didTapBaseCurrency() }

        language.setOnClickListener { presenter.didTapLanguage() }

        lightMode.setOnClickListener { lightMode.switchToggle() }

        about.setOnClickListener { presenter.didTapAbout() }

        report.setOnClickListener { presenter.didTapReportProblem() }

        shareApp.setOnClickListener { presenter.didTapTellFriends() }

        companyLogo.setOnClickListener { presenter.didTapCompanyLogo() }
    }

    private fun subscribeToViewEvents(presenterView: MainSettingsView, presenter: MainSettingsPresenter) {
        presenterView.baseCurrency.observe(viewLifecycleOwner, Observer { currency ->
            currency?.let {
                baseCurrency.rightTitle = it
            }
        })

        presenterView.backedUp.observe(viewLifecycleOwner, Observer { wordListBackedUp ->
            securityCenter.badgeImage = !wordListBackedUp
        })

        presenterView.language.observe(viewLifecycleOwner, Observer { languageCode ->
            languageCode?.let {
                language.rightTitle = it
            }
        })

        presenterView.lightMode.observe(viewLifecycleOwner, Observer { lightModeValue ->
            lightModeValue?.let {
                lightMode.apply {
                    switchIsChecked = it

                    switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                        presenter.didSwitchLightMode(isChecked)
                    }
                }
            }
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
    }

    private fun subscribeToRouterEvents(router: MainSettingsRouter) {
        router.showBaseCurrencySettingsLiveEvent.observe(viewLifecycleOwner, Observer {
            context?.let { context -> BaseCurrencySettingsModule.start(context) }
        })

        router.showLanguageSettingsLiveEvent.observe(viewLifecycleOwner, Observer {
            context?.let { context -> LanguageSettingsModule.start(context) }
        })

        router.showAboutLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.let {
                AboutSettingsActivity.start(it)
            }
        })

        router.showNotificationsLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.let {
                NotificationsModule.start(it)
            }
        })

        router.showReportProblemLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.let {
                ReportProblemModule.start(it)
            }
        })

        router.showSecuritySettingsLiveEvent.observe(viewLifecycleOwner, Observer {
            context?.let {
                SecuritySettingsModule.start(it)
            }
        })

        router.showManageCoinsLiveEvent.observe(viewLifecycleOwner, Observer {
            context?.let { ManageWalletsModule.start(it, false) }
        })

        router.showExperimentalFeaturesLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.let { ExperimentalFeaturesModule.start(it) }
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
            activity?.let { MainModule.startAsNewTask(it, MainActivity.SETTINGS_TAB_POSITION) }
        })
    }
}
