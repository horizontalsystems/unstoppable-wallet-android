package io.horizontalsystems.bankwallet.modules.settings.main

import android.content.Intent
import android.net.Uri
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
import io.horizontalsystems.bankwallet.modules.settings.AboutSettingsActivity
import io.horizontalsystems.bankwallet.modules.settings.basecurrency.BaseCurrencySettingsModule
import io.horizontalsystems.bankwallet.modules.settings.language.LanguageSettingsModule
import io.horizontalsystems.bankwallet.modules.settings.security.SecuritySettingsModule
import kotlinx.android.synthetic.main.fragment_settings.*


class MainSettingsFragment : Fragment() {
    private lateinit var viewModel: MainSettingsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(MainSettingsViewModel::class.java)
        viewModel.init()

        shadowlessToolbar.bindTitle(getString(R.string.Settings_Title))

        securityCenter.apply {
            showArrow()
            setOnClickListener { viewModel.delegate.didTapSecurity() }
        }

        baseCurrency.apply {
            showArrow()
            setOnClickListener { viewModel.delegate.didTapBaseCurrency() }
        }

        language.apply {
            showArrow()
            setOnClickListener { viewModel.delegate.didTapLanguage() }
        }

        lightMode.apply {
            setOnClickListener { switchToggle() }
        }

        companyLogo.setOnClickListener {
            viewModel.delegate.didTapAppLink()
        }

        about.apply {
            showArrow()
            setOnClickListener { viewModel.delegate.didTapAbout() }
        }

        shareApp.setOnClickListener {
            shareAppLink()
        }

        viewModel.baseCurrencyLiveDate.observe(viewLifecycleOwner, Observer { currency ->
            currency?.let {
                baseCurrency.selectedValue = it
            }
        })

        viewModel.backedUpLiveDate.observe(viewLifecycleOwner, Observer { wordListBackedUp ->
            wordListBackedUp?.let {wordListIsBackedUp ->
                securityCenter.setInfoBadgeVisibility(!wordListIsBackedUp)
            }
        })

        viewModel.showBaseCurrencySettingsLiveEvent.observe(viewLifecycleOwner, Observer {
            context?.let { context -> BaseCurrencySettingsModule.start(context) }
        })

        viewModel.languageLiveDate.observe(viewLifecycleOwner, Observer { languageCode ->
            languageCode?.let {
                language.selectedValue = it.capitalize()
            }
        })

        viewModel.lightModeLiveDate.observe(viewLifecycleOwner, Observer { lightModeValue ->
            lightModeValue?.let {
                lightMode.apply {
                    switchIsChecked = it

                    switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                        viewModel.delegate.didSwitchLightMode(isChecked)
                    }
                }
            }
        })

        viewModel.showLanguageSettingsLiveEvent.observe(viewLifecycleOwner, Observer {
            context?.let { context -> LanguageSettingsModule.start(context) }
        })

        viewModel.showAboutLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.let {
                AboutSettingsActivity.start(it)
            }
        })

        viewModel.showSecuritySettingsLiveEvent.observe(viewLifecycleOwner, Observer {
            context?.let {
                SecuritySettingsModule.start(it)
            }
        })

        viewModel.tabItemBadgeLiveDate.observe(viewLifecycleOwner, Observer { count ->
            activity?.let { activity ->
                count?.let {
                    (activity as? MainActivity)?.updateSettingsTabCounter(it)
                }
            }
        })

        viewModel.appVersionLiveDate.observe(viewLifecycleOwner, Observer { version ->
            version?.let {
                var appVersion = getString(R.string.Settings_InfoTitleWithVersion, it)
                if (getString(R.string.is_release) == "false") {
                    appVersion = "$appVersion (${BuildConfig.VERSION_CODE})"
                }
                appName.text = appVersion
            }
        })

        viewModel.showAppLinkLiveEvent.observe(viewLifecycleOwner, Observer {
            val uri = Uri.parse(getString(R.string.Settings_InfoLink))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            activity?.startActivity(intent)
        })

        viewModel.reloadAppLiveEvent.observe(viewLifecycleOwner, Observer {
            context?.let { context -> MainModule.startAsNewTask(context, MainActivity.SETTINGS_TAB_POSITION) }
        })

    }

    private fun shareAppLink() {
        val shareMessage = getString(R.string.SettingsShare_Text) + "\n" + getString(R.string.SettingsShare_Link) + "\n"

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.SettingsShare_Title)))
    }

}
