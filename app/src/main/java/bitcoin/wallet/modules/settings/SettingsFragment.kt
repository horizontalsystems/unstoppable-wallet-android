package bitcoin.wallet.modules.settings

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import bitcoin.wallet.R
import bitcoin.wallet.core.App
import bitcoin.wallet.modules.currencyswitcher.CurrencySwitcherModule
import bitcoin.wallet.modules.main.MainActivity
import bitcoin.wallet.modules.main.MainModule
import bitcoin.wallet.modules.settings.language.LanguageSettingsModule
import kotlinx.android.synthetic.main.fragment_settings.*


class SettingsFragment : android.support.v4.app.Fragment() {

    private lateinit var viewModel: SettingsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(SettingsViewModel::class.java)
        viewModel.init()

        toolbar.setTitle(R.string.settings_title)

        securityCenter.setOnClickListener {
            startActivity(Intent(context, SecuritySettingsActivity::class.java))
        }


        baseCurrency.apply {
            setOnClickListener {
                CurrencySwitcherModule.start(context)
            }
        }

        language.apply {
            setOnClickListener {
                LanguageSettingsModule.start(context)
            }
            selectedValue = App.languageManager.currentLanguage.displayName.capitalize()
        }

        lightMode.apply {
            switchIsChecked = App.localStorage.isLightModeOn
            setOnClickListener {
                switchToggle()
            }

            switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                App.localStorage.isLightModeOn = isChecked
                MainModule.startAsNewTask(context, MainActivity.SETTINGS_TAB_POSITION)
            }
        }

        about.setOnClickListener {
            Log.e("AAA", "About clicked!")
        }

        companyLogo.setOnClickListener {
            val uri = Uri.parse(getString(R.string.settings_info_link))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            activity?.startActivity(intent)
        }

        val appVersion = "1.01"
        appName.text = getString(R.string.settings_info_app_name_with_version, appVersion)

        viewModel.wordListBackedUp.observe(this, Observer { wordListBackedUp ->
            wordListBackedUp?.let {
                securityCenter.badge = getInfoBadge(it)
            }
        })

        viewModel.baseCurrencyCode.observe(this, Observer { code ->
            code?.let {
                baseCurrency.selectedValue = it
            }
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
