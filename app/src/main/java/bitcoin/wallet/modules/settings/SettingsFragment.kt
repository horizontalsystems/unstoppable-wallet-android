package bitcoin.wallet.modules.settings

import android.content.Intent
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

        toolbar.setTitle(R.string.tab_title_settings)

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
            switchIsChecked = Factory.preferencesManager.isLightModeEnabled
            setOnClickListener {
                switchToggle()
            }

            switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                Factory.preferencesManager.isLightModeEnabled = isChecked
                activity?.recreate()
            }
        }

        pushNotifications.apply {
            switchIsChecked = true
            setOnClickListener {
                switchToggle()
            }
            switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                Log.e("AAA", "pushNotifications $isChecked")
            }
        }

        appVersion.apply {
            selectedValue = "1.01"
            setOnClickListener {
                Log.e("AAA", "app version clicked!")
            }
        }

    }

}
