package io.horizontalsystems.bankwallet.modules.settings.security

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.settings.managekeys.ManageKeysModule
import io.horizontalsystems.bankwallet.ui.dialogs.AlertDialogFragment
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import kotlinx.android.synthetic.main.activity_settings_security.*

class SecuritySettingsActivity : BaseActivity() {

    private lateinit var viewModel: SecuritySettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_security)

        viewModel = ViewModelProviders.of(this).get(SecuritySettingsViewModel::class.java)
        viewModel.init()

        shadowlessToolbar.bind(getString(R.string.Settings_SecurityCenter), TopMenuItem(R.drawable.back, onClick = { onBackPressed() }))

        changePin.setOnClickListener { viewModel.delegate.didTapEditPin() }

        manageKeys.setOnClickListener { viewModel.delegate.didTapManageKeys() }

        //  Handling view model live events

        viewModel.backedUpLiveData.observe(this, Observer { wordListBackedUp ->
            manageKeys.setInfoBadgeVisibility(!wordListBackedUp)
        })

        viewModel.openManageKeysLiveEvent.observe(this, Observer {
            ManageKeysModule.start(this)
        })

        viewModel.pinEnabledLiveEvent.observe(this, Observer { pinEnabled ->
            enablePin.apply {
                switchIsChecked = pinEnabled
                setOnClickListener {
                    switchToggle()
                }
                switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                    viewModel.delegate.didTapEnablePin(isChecked)
                }
            }
            changePin.visibility = if (pinEnabled) View.VISIBLE else View.GONE
        })

        viewModel.openEditPinLiveEvent.observe(this, Observer {
            PinModule.startForEditPin(this)
        })

        viewModel.openSetPinLiveEvent.observe(this, Observer {
            PinModule.startForSetPin(this, REQUEST_CODE_SET_PIN)
        })

        viewModel.openUnlockPinLiveEvent.observe(this, Observer {
            PinModule.startForUnlock(this, REQUEST_CODE_UNLOCK_PIN_TO_DISABLE_PIN, true)
        })

        viewModel.showFingerprintSettings.observe(this, Observer { enabled ->
            fingerprint.apply {
                switchIsChecked = enabled

                setOnClickListener {
                    switchToggle()
                }

                switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                    viewModel.delegate.didTapEnableFingerprint(isChecked)
                }
                visibility = View.VISIBLE
            }
        })

        viewModel.hideFingerprintSettings.observe(this, Observer {
            fingerprint.visibility = View.GONE
        })

        viewModel.showNoEnrolledFingerprints.observe(this, Observer {
            AlertDialogFragment
                    .newInstance(R.string.Settings_Error_FingerprintNotEnabled, R.string.Settings_Error_NoFingerprintAddedYet, R.string.Alert_Ok)
                    .show(this.supportFragmentManager, "fingerprint_not_enabled_alert")
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SET_PIN) {
            when (resultCode) {
                PinModule.RESULT_OK -> viewModel.delegate.didSetPin()
                PinModule.RESULT_CANCELLED -> viewModel.delegate.didCancelSetPin()
            }
        }

        if (requestCode == REQUEST_CODE_UNLOCK_PIN_TO_DISABLE_PIN) {
            when (resultCode) {
                PinModule.RESULT_OK -> viewModel.delegate.didUnlockPinToDisablePin()
                PinModule.RESULT_CANCELLED -> viewModel.delegate.didCancelUnlockPinToDisablePin()
            }
        }
    }

    companion object {
        const val REQUEST_CODE_SET_PIN = 1
        const val REQUEST_CODE_UNLOCK_PIN_TO_DISABLE_PIN = 2
    }
}
