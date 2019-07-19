package io.horizontalsystems.bankwallet.modules.settings.security

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.BiometryType
import io.horizontalsystems.bankwallet.lib.AlertDialogFragment
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.settings.managekeys.ManageKeysModule
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import kotlinx.android.synthetic.main.activity_settings_security.*

class SecuritySettingsActivity : BaseActivity() {

    private lateinit var viewModel: SecuritySettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_security)

        viewModel = ViewModelProviders.of(this).get(SecuritySettingsViewModel::class.java)
        viewModel.init()

        shadowlessToolbar.bind(getString(R.string.Settings_SecurityCenter), TopMenuItem(R.drawable.back) { onBackPressed() })

        changePin.apply {
            showArrow()
            setOnClickListener { viewModel.delegate.didTapEditPin() }
        }

        manageKeys.apply {
            showArrow()
            setOnClickListener { viewModel.delegate.didTapManageKeys() }
        }

        //  Handling view model live events

        viewModel.openManageKeysLiveEvent.observe(this, Observer {
            ManageKeysModule.start(this)
        })

        viewModel.openEditPinLiveEvent.observe(this, Observer {
            PinModule.startForEditPin(this)
        })

        viewModel.biometryTypeLiveData.observe(this, Observer { biometryType ->
            fingerprint.visibility = if (biometryType == BiometryType.FINGER) View.VISIBLE else View.GONE
        })

        viewModel.biometricUnlockOnLiveData.observe(this, Observer { switchIsOn ->
            switchIsOn?.let { switchOn ->
                fingerprint.apply {
                    switchIsChecked = switchOn
                    setOnClickListener {
                        if (App.localStorage.isBiometricOn || fingerprintCanBeEnabled()) {
                            switchToggle()
                        }
                    }

                    switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                        App.localStorage.isBiometricOn = isChecked
                    }
                }
            }
        })

        viewModel.backedUpLiveData.observe(this, Observer { wordListBackedUp ->
            wordListBackedUp?.let { wordListIsBackedUp ->
                manageKeys.setInfoBadgeVisibility(!wordListIsBackedUp)
            }
        })
    }

    private fun fingerprintCanBeEnabled(): Boolean {
        val touchSensorCanBeUsed = App.systemInfoManager.touchSensorCanBeUsed()
        if (!touchSensorCanBeUsed) {
            AlertDialogFragment
                    .newInstance(R.string.Settings_Error_FingerprintNotEnabled, R.string.Settings_Error_NoFingerprintAddedYet, R.string.Alert_Ok)
                    .show(this.supportFragmentManager, "fingerprint_not_enabled_alert")
            return false
        }
        return true
    }
}
