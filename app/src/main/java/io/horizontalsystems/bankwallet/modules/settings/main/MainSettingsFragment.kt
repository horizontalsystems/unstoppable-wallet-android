package io.horizontalsystems.bankwallet.modules.settings.main

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.settings.AboutSettingsActivity
import io.horizontalsystems.bankwallet.modules.settings.basecurrency.BaseCurrencySettingsModule
import io.horizontalsystems.bankwallet.modules.settings.language.LanguageSettingsModule
import io.horizontalsystems.bankwallet.modules.settings.security.SecuritySettingsModule
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import kotlinx.android.synthetic.main.fragment_settings.*

class MainSettingsFragment : android.support.v4.app.Fragment() {
    private lateinit var viewModel: MainSettingsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(MainSettingsViewModel::class.java)
        viewModel.init()

        securityCenter.setOnClickListener {
            viewModel.delegate.didTapSecurity()
        }

        baseCurrency.apply {
            setOnClickListener {
                viewModel.delegate.didTapBaseCurrency()
            }
        }

        language.apply {
            setOnClickListener {
                viewModel.delegate.didTapLanguage()
            }
        }

        lightMode.apply {
            setOnClickListener {
                switchToggle()
            }
        }

        companyLogo.setOnClickListener {
            viewModel.delegate.didTapAppLink()
        }

        about.setOnClickListener {
            viewModel.delegate.didTapAbout()
        }



        viewModel.titleLiveDate.observe(this, Observer { title ->
            title?.let { toolbar.setTitle(it) }
        })

        viewModel.baseCurrencyLiveDate.observe(this, Observer { currency ->
            currency?.let {
                baseCurrency.selectedValue = it
            }
        })

        viewModel.backedUpLiveDate.observe(this, Observer { backedUp ->
            backedUp?.let {
                securityCenter.badge = LayoutHelper.getInfoBadge(it, resources)
            }
        })

        viewModel.showBaseCurrencySettingsLiveEvent.observe(this, Observer {
            context?.let { context -> BaseCurrencySettingsModule.start(context) }
        })

        viewModel.languageLiveDate.observe(this, Observer { languageCode ->
            languageCode?.let {
                language.selectedValue = it
            }
        })

        viewModel.lightModeLiveDate.observe(this, Observer { lightModeValue ->
            lightModeValue?.let {
                lightMode.apply {
                    switchIsChecked = it

                    switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                        viewModel.delegate.didSwitchLightMode(isChecked)
                    }
                }
            }
        })

        viewModel.showLanguageSettingsLiveEvent.observe(this, Observer {
            context?.let { context -> LanguageSettingsModule.start(context) }
        })

        viewModel.showAboutLiveEvent.observe(this, Observer {
            activity?.let {
                AboutSettingsActivity.start(it)
            }
        })

        viewModel.showSecuritySettingsLiveEvent.observe(this, Observer {
            context?.let {
                SecuritySettingsModule.start(it)
            }
        })

        viewModel.tabItemBadgeLiveDate.observe(this, Observer { count ->
            activity?.let { activity ->
                count?.let {
                    (activity as? MainActivity)?.updateSettingsTabCounter(it)
                }
            }
        })

        viewModel.appVersionLiveDate.observe(this, Observer { version ->
            version?.let { appName.text = getString(R.string.settings_info_app_name_with_version, it) }
        })

        viewModel.showAppLinkLiveEvent.observe(this, Observer {
            val uri = Uri.parse(getString(R.string.settings_info_link))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            activity?.startActivity(intent)
        })

        viewModel.reloadAppLiveEvent.observe(this, Observer {
            context?.let { context -> MainModule.startAsNewTask(context, MainActivity.SETTINGS_TAB_POSITION) }
        })

    }

}
