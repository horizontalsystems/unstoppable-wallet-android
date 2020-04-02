package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.content.Intent
import android.os.Bundle
import android.widget.CompoundButton
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SelectorDialog
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SelectorItem
import io.horizontalsystems.bankwallet.modules.tor.TorConnectionActivity
import io.horizontalsystems.views.AlertDialogFragment
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.activity_settings_privacy.*
import kotlinx.android.synthetic.main.activity_settings_security.shadowlessToolbar
import kotlin.system.exitProcess

class PrivacySettingsActivity : BaseActivity(), SelectorDialog.Listener {
    private lateinit var viewModel: PrivacySettingsViewModel
    private lateinit var communicationSettingsAdapter: PrivacySettingsAdapter
    private lateinit var walletRestoreSettingsAdapter: PrivacySettingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_privacy)

        viewModel = ViewModelProvider(this).get(PrivacySettingsViewModel::class.java)
        viewModel.init()

        communicationSettingsAdapter = PrivacySettingsAdapter(viewModel.delegate)
        walletRestoreSettingsAdapter = PrivacySettingsAdapter(viewModel.delegate)

        communicationSettings.adapter = communicationSettingsAdapter
        walletRestoreSettings.adapter = walletRestoreSettingsAdapter

        shadowlessToolbar.bind(getString(R.string.SettingsSecurity_Privacy), TopMenuItem(R.drawable.ic_back, onClick = { onBackPressed() }))

        torConnectionSwitch.switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            viewModel.delegate.didSwitchTorEnabled(isChecked)
        }

        // IView
        viewModel.torEnabledLiveData.observe(this, Observer { enabled ->
            torConnectionSwitch.switchIsChecked = enabled
            torConnectionSwitch.subtitle = if (enabled) getString(R.string.SettingsOption_Enabled) else getString(R.string.SettingsOption_Disabled)
        })

        viewModel.showAppRestartAlertForTor.observe(this, Observer { checked ->
            showAppRestartAlert(checked)
        })

        viewModel.showNotificationsNotEnabledAlert.observe(this, Observer {
            showNotificationsNotEnabledAlert()
        })

        viewModel.communicationSettingsViewItems.observe(this, Observer {
            communicationSettingsAdapter.items = it
            communicationSettingsAdapter.notifyDataSetChanged()
        })

        viewModel.restoreWalletSettingsViewItems.observe(this, Observer {
            walletRestoreSettingsAdapter.items = it
            walletRestoreSettingsAdapter.notifyDataSetChanged()
        })

        viewModel.showSyncModeSelectorDialog.observe(this, Observer { (items, selected) ->
            SelectorDialog.newInstance(this, items.map { SelectorItem(it.title, it == selected) }, null, false)
                    .show(supportFragmentManager, "syncmode_settings_selector")
        })

        viewModel.showCommunicationSelectorDialog.observe(this, Observer { (items, selected) ->
            SelectorDialog.newInstance(this, items.map { SelectorItem(it.title, it == selected) }, null, false)
                    .show(supportFragmentManager, "communication_mode_selector")
        })

        viewModel.showRestoreModeChangeAlert.observe(this, Observer { (coin, syncMode) ->
            PrivacySettingsAlertDialog.show(
                    title = getString(R.string.BlockchainSettings_SyncModeChangeAlert_Title),
                    subtitle = syncMode.title,
                    contentText = getString(R.string.BlockchainSettings_SyncModeChangeAlert_Content, coin.title),
                    actionButtonTitle = getString(R.string.Button_Change),
                    activity = this,
                    listener = object : PrivacySettingsAlertDialog.Listener {
                        override fun onActionButtonClick() {
                            viewModel.delegate.proceedWithSyncModeChange(coin, syncMode)
                        }
                    }
            )
        })

        viewModel.showCommunicationModeChangeAlert.observe(this, Observer { (coin, communicationMode) ->
            PrivacySettingsAlertDialog.show(
                    title = getString(R.string.BlockchainSettings_CommunicationModeChangeAlert_Title),
                    subtitle = communicationMode.title,
                    contentText = getString(R.string.BlockchainSettings_CommunicationModeChangeAlert_Content, coin.title),
                    actionButtonTitle = getString(R.string.Button_Change),
                    activity = this,
                    listener = object : PrivacySettingsAlertDialog.Listener {
                        override fun onActionButtonClick() {
                            viewModel.delegate.proceedWithCommunicationModeChange(coin, communicationMode)
                        }
                    }
            )
        })

        // IRouter
        viewModel.restartApp.observe(this, Observer {
            restartApp()
        })
    }

    override fun onSelectItem(position: Int) {
        viewModel.delegate.onSelectSetting(position)
    }

    private fun showAppRestartAlert(checked: Boolean) {
        AlertDialogFragment.newInstance(
                descriptionString = getString(R.string.SettingsSecurity_AppRestartWarning),
                buttonText = R.string.Alert_Restart,
                cancelButtonText = R.string.Alert_Cancel,
                cancelable = true,
                listener = object : AlertDialogFragment.Listener {
                    override fun onButtonClick() {
                        viewModel.delegate.setTorEnabled(checked)
                    }

                    override fun onCancel() {
                        torConnectionSwitch.switchIsChecked = !checked
                    }
                }).show(supportFragmentManager, "alert_dialog")
    }

    private fun showNotificationsNotEnabledAlert() {
        AlertDialogFragment.newInstance(
                descriptionString = getString(R.string.SettingsSecurity_NotificationsDisabledWarning),
                buttonText = R.string.Button_Enable,
                cancelButtonText = R.string.Alert_Cancel,
                cancelable = true,
                listener = object : AlertDialogFragment.Listener {
                    override fun onButtonClick() {
                        openAppNotificationSettings()
                    }

                    override fun onCancel() {
                        torConnectionSwitch.switchIsChecked = false
                    }
                }).show(supportFragmentManager, "alert_dialog_notification")
    }

    private fun openAppNotificationSettings() {
        val intent = Intent()
        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        intent.putExtra("android.provider.extra.APP_PACKAGE", packageName)
        startActivity(intent)
    }

    private fun restartApp() {
        MainModule.startAsNewTask(this)
        if (App.localStorage.torEnabled) {
            val intent = Intent(this, TorConnectionActivity::class.java)
            startActivity(intent)
        }
        exitProcess(0)
    }
}
