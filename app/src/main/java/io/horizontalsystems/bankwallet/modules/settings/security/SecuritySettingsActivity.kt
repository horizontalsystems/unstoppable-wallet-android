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
import io.horizontalsystems.bankwallet.modules.backup.BackupModule
import io.horizontalsystems.bankwallet.modules.backup.BackupPresenter
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.restore.RestoreModule
import io.horizontalsystems.bankwallet.modules.settings.managekeys.ManageKeysModule
import io.horizontalsystems.bankwallet.ui.dialogs.BottomConfirmAlert
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import kotlinx.android.synthetic.main.activity_settings_security.*

class SecuritySettingsActivity : BaseActivity(), BottomConfirmAlert.Listener {

    private lateinit var viewModel: SecuritySettingsViewModel

    enum class Action {
        OPEN_RESTORE,
        CLEAR_WALLETS
    }

    private var selectedAction: Action? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_security)

        viewModel = ViewModelProviders.of(this).get(SecuritySettingsViewModel::class.java)
        viewModel.init()

        shadowlessToolbar.bind(title = getString(R.string.Settings_SecurityCenter), leftBtnItem = TopMenuItem(R.drawable.back) { onBackPressed() })
        changePin.setOnClickListener { viewModel.delegate.didTapEditPin() }
        manageKeys.setOnClickListener { viewModel.delegate.didTapManageKeys() }

        viewModel.openManageKeysLiveEvent.observe(this, Observer {
            ManageKeysModule.start(this)
        })

        viewModel.openEditPinLiveEvent.observe(this, Observer {
            PinModule.startForEditPin(this)
        })

        viewModel.openRestoreWalletLiveEvent.observe(this, Observer {
            RestoreModule.start(this)
        })

        viewModel.openBackupWalletLiveEvent.observe(this, Observer {
            BackupModule.start(this@SecuritySettingsActivity, BackupPresenter.DismissMode.DISMISS_SELF)
        })

        viewModel.biometryTypeLiveDate.observe(this, Observer { biometryType ->
            fingerprint.visibility = if (biometryType == BiometryType.FINGER) View.VISIBLE else View.GONE
        })

        viewModel.biometricUnlockOnLiveDate.observe(this, Observer { switchIsOn ->
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

        viewModel.reloadAppLiveEvent.observe(this, Observer {
            MainModule.startAsNewTask(this)
        })

        viewModel.showPinUnlockLiveEvent.observe(this, Observer {
            PinModule.startForUnlock(this, true)
        })

        viewModel.backedUpLiveData.observe(this, Observer {
            manageKeys.badge = LayoutHelper.getInfoBadge(it, resources)
        })
    }

    override fun onConfirmationSuccess() {
        when (selectedAction) {
            Action.OPEN_RESTORE -> viewModel.delegate.didTapRestoreWallet()
            Action.CLEAR_WALLETS -> viewModel.delegate.confirmedUnlinkWallet()
        }
    }

    private fun fingerprintCanBeEnabled(): Boolean {
        val touchSensorCanBeUsed = App.systemInfoManager.touchSensorCanBeUsed()
        if (!touchSensorCanBeUsed) {
            AlertDialogFragment.newInstance(R.string.Settings_Error_FingerprintNotEnabled, R.string.Settings_Error_NoFingerprintAddedYet, R.string.Alert_Ok)
                    .show(this.supportFragmentManager, "fingerprint_not_enabled_alert")
            return false
        }
        return true
    }
}
