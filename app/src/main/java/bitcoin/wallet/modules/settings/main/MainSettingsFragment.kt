package bitcoin.wallet.modules.settings.main

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import bitcoin.wallet.R
import bitcoin.wallet.modules.currencyswitcher.CurrencySwitcherModule
import bitcoin.wallet.modules.main.MainActivity
import bitcoin.wallet.modules.main.MainModule
import bitcoin.wallet.modules.settings.SecuritySettingsActivity
import bitcoin.wallet.modules.settings.language.LanguageSettingsModule
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
            startActivity(Intent(context, SecuritySettingsActivity::class.java))
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
                securityCenter.badge = getInfoBadge(it)
            }
        })

        viewModel.showBaseCurrencySettingsLiveEvent.observe(this, Observer {
            context?.let { context -> CurrencySwitcherModule.start(context) }
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
            context?.let {
                //open About page
            }
        })

        viewModel.showSecuritySettingsLiveEvent.observe(this, Observer {
            context?.let {
                startActivity(Intent(context, SecuritySettingsActivity::class.java))
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

    private fun getInfoBadge(wordListBackedUp: Boolean): Drawable? {
        var infoBadge: Drawable? = null
        if (!wordListBackedUp) {
            infoBadge = resources.getDrawable(R.drawable.info, null)
            infoBadge?.setTint(resources.getColor(R.color.red_warning, null))
        }
        return infoBadge
    }

}
