package io.horizontalsystems.bankwallet.modules.settings.security

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.LauncherActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.BiometryType
import io.horizontalsystems.bankwallet.lib.AlertDialogFragment
import io.horizontalsystems.bankwallet.modules.backup.BackupModule
import io.horizontalsystems.bankwallet.modules.backup.BackupPresenter
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.restore.RestoreModule
import io.horizontalsystems.bankwallet.ui.dialogs.BottomButtonColor
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

        viewModel = ViewModelProviders.of(this).get(SecuritySettingsViewModel::class.java)
        viewModel.init()

        setContentView(R.layout.activity_settings_security)

        shadowlessToolbar.bind(
                title = getString(R.string.Settings_SecurityCenter),
                leftBtnItem = TopMenuItem(R.drawable.back, { onBackPressed() })
        )

        changePin.setOnClickListener { viewModel.delegate.didTapEditPin() }

        backupWallet.setOnClickListener { viewModel.delegate.didTapBackupWallet() }


        importWallet.setOnClickListener {
            selectedAction = Action.OPEN_RESTORE
            val confirmationList = mutableListOf(
                    R.string.SettingsSecurity_ImportWalletConfirmation_1,
                    R.string.SettingsSecurity_ImportWalletConfirmation_2
            )
            BottomConfirmAlert.show(this, confirmationList, this)
        }

        unlink.setOnClickListener {
            selectedAction = Action.CLEAR_WALLETS
            val confirmationList = mutableListOf(
                    R.string.SettingsSecurity_ImportWalletConfirmation_1,
                    R.string.SettingsSecurity_ImportWalletConfirmation_2
            )
            BottomConfirmAlert.show(this, confirmationList, this, BottomButtonColor.RED)
        }

        unlink.titleTextColor = R.color.red_warning


        viewModel.backedUpLiveData.observe(this, Observer { wordListBackedUp ->
            wordListBackedUp?.let {
                backupWallet.badge = LayoutHelper.getInfoBadge(it, resources)
            }
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
            val intent = Intent(this, LauncherActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        })

        viewModel.showPinUnlockLiveEvent.observe(this, Observer {
            PinModule.startForUnlock(true)
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
