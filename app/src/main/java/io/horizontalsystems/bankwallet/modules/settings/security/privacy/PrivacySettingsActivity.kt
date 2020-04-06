package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.entities.TransactionDataSortingType
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SelectorDialog
import io.horizontalsystems.bankwallet.modules.send.submodules.hodler.SelectorItem
import io.horizontalsystems.bankwallet.modules.tor.TorConnectionActivity
import io.horizontalsystems.views.AlertDialogFragment
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.activity_settings_privacy.*
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

        communicationSettingsRecyclerview.adapter = communicationSettingsAdapter
        walletRestoreSettingsRecyclerview.adapter = walletRestoreSettingsAdapter

        shadowlessToolbar.bind(getString(R.string.SettingsSecurity_Privacy), TopMenuItem(R.drawable.ic_back, onClick = { onBackPressed() }))

        torConnectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.delegate.didSwitchTorEnabled(isChecked)
        }

        transactionsOrderSetting.apply {
            dropDownArrow = true
            setOnClickListener {
                viewModel.delegate.onTransactionOrderSettingTap()
            }
        }

        // IView
        viewModel.torEnabledLiveData.observe(this, Observer { enabled ->
            setTorSwitch(enabled)
        })

        viewModel.setTorConnectionStatus.observe(this, Observer { torStatus ->
            torStatus?.let {
                when (torStatus) {
                    TorStatus.Connecting -> {
                        connectionSpinner.visibility = View.VISIBLE
                        controlIcon.setTint(getTint(R.color.grey))
                        controlIcon.bind(R.drawable.ic_tor_connected)
                        subtitleText.text = getString(R.string.TorPage_Connecting)
                    }
                    TorStatus.Connected -> {
                        connectionSpinner.visibility = View.GONE
                        controlIcon.setTint(getTint(R.color.yellow_d))
                        controlIcon.bind(R.drawable.ic_tor_connected)
                        subtitleText.text = getString(R.string.TorPage_Connected)
                    }
                    TorStatus.Failed -> {
                        connectionSpinner.visibility = View.GONE
                        controlIcon.setTint(getTint(R.color.yellow_d))
                        controlIcon.bind(R.drawable.ic_tor_status_error)
                        subtitleText.text = getString(R.string.TorPage_Failed)
                    }
                    TorStatus.Closed -> {
                        connectionSpinner.visibility = View.GONE
                        controlIcon.setTint(getTint(R.color.yellow_d))
                        controlIcon.bind(R.drawable.ic_tor)
                        subtitleText.text = getString(R.string.TorPage_ConnectionClosed)
                    }
                }
            }
        })

        viewModel.transactionOrderingLiveData.observe(this, Observer { ordering ->
            transactionsOrderSetting.dropDownText = getSortingLocalized(ordering)
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

        viewModel.showTransactionsSortingSelectorDialog.observe(this, Observer { (items, selected) ->
            SelectorDialog.newInstance(
                    items = items.map { SelectorItem(getSortingLocalized(it), it == selected) },
                    toggleKeyboard = false,
                    listener = object : SelectorDialog.Listener {
                        override fun onSelectItem(position: Int) {
                            viewModel.delegate.onSelectTransactionSorting(items[position])
                        }
                    })
                    .show(supportFragmentManager, "transactions_sorting_settings_selector")
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

    private fun getSortingLocalized(sortingType: TransactionDataSortingType): String{
        return when(sortingType) {
            TransactionDataSortingType.Shuffle -> getString(R.string.SettingsSecurity_SortingShuffle)
            TransactionDataSortingType.Bip69 -> getString(R.string.SettingsSecurity_SortingBip69)
            TransactionDataSortingType.Off -> getString(R.string.SettingsSecurity_SortingOff)
        }
    }

    private fun setTorSwitch(checked: Boolean) {
        torConnectionSwitch.setOnCheckedChangeListener(null)
        torConnectionSwitch.isChecked = checked
        torConnectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.delegate.didSwitchTorEnabled(isChecked)
        }
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
                        setTorSwitch(!checked)
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
                        setTorSwitch(false)
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

    private fun getTint(color: Int) = ColorStateList.valueOf(ContextCompat.getColor(this, color))

}
