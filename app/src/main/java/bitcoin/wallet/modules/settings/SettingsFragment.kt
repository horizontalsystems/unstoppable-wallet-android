package bitcoin.wallet.modules.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import bitcoin.wallet.R
import bitcoin.wallet.core.managers.Factory
import kotlinx.android.synthetic.main.fragment_settings.*


class SettingsFragment : android.support.v4.app.Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setTitle(R.string.settings_title)

        resources.getDrawable(R.drawable.info, null).let { securityBadge ->
            securityBadge.setTint(resources.getColor(R.color.red_warning, null))
            securityCenter.badge = securityBadge
        }

        securityCenter.setOnClickListener {
            startActivity(Intent(context, SecuritySettingsActivity::class.java))
        }


        baseCurrency.apply {
            setOnClickListener {
                Log.e("AAA", "base currency clicked!")
            }
            selectedValue = "USD"
        }

        importWallet.setOnClickListener {
            Log.e("AAA", "import wallet clicked!")
        }

        language.apply {
            setOnClickListener {
                Log.e("AAA", "language clicked!")
            }
            selectedValue = "English"
        }

        lightMode.apply {
            switchIsChecked = Factory.preferencesManager.isLightModeEnabled()
            setOnClickListener {
                switchToggle()
            }

            switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                Factory.preferencesManager.setLightModeEnabled(isChecked)
                activity?.recreate()
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

    }

}
