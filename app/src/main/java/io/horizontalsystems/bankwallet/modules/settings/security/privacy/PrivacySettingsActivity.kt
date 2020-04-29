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
import io.horizontalsystems.bankwallet.entities.CommunicationMode
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.TransactionDataSortingType
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.tor.TorConnectionActivity
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.OnItemSelectedListener
import io.horizontalsystems.views.AlertDialogFragment
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.activity_settings_privacy.*
import kotlin.system.exitProcess

class PrivacySettingsActivity : BaseActivity() {
    private lateinit var viewModel: PrivacySettingsViewModel
    private lateinit var communicationSettingsAdapter: PrivacySettingsAdapter
    private lateinit var walletRestoreSettingsAdapter: PrivacySettingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_privacy)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this).get(PrivacySettingsViewModel::class.java)
        viewModel.init()

        // Always show Communication settings
        createCommunicationSettingsView(true)
        // Do not create Wallet restore settings view if Wallet is created or started first time

        torConnectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.delegate.didSwitchTorEnabled(isChecked)
        }

        transactionsOrderSetting.setOnClickListener {
            viewModel.delegate.onTransactionOrderSettingTap()
        }

        // IView
        viewModel.torEnabledLiveData.observe(this, Observer { enabled ->
            setTorSwitch(enabled)
        })

        viewModel.blockchainSettingsVisibilityLiveData.observe(this, Observer { isVisible ->
            createWalletRestoreSettingsView(isVisible)
        })

        viewModel.setTorConnectionStatus.observe(this, Observer { torStatus ->
            torStatus?.let {
                when (torStatus) {
                    TorStatus.Connecting -> {
                        connectionSpinner.visibility = View.VISIBLE
                        controlIcon.imageTintList = getTint(R.color.grey)
                        controlIcon.setImageResource(R.drawable.ic_tor_connected)
                        subtitleText.text = getString(R.string.TorPage_Connecting)
                    }
                    TorStatus.Connected -> {
                        connectionSpinner.visibility = View.GONE
                        controlIcon.imageTintList = getTint(R.color.yellow_d)
                        controlIcon.setImageResource(R.drawable.ic_tor_connected)
                        subtitleText.text = getString(R.string.TorPage_Connected)
                    }
                    TorStatus.Failed -> {
                        connectionSpinner.visibility = View.GONE
                        controlIcon.imageTintList = getTint(R.color.yellow_d)
                        controlIcon.setImageResource(R.drawable.ic_tor_status_error)
                        subtitleText.text = getString(R.string.TorPage_Failed)
                    }
                    TorStatus.Closed -> {
                        connectionSpinner.visibility = View.GONE
                        controlIcon.imageTintList = getTint(R.color.yellow_d)
                        controlIcon.setImageResource(R.drawable.ic_tor)
                        subtitleText.text = getString(R.string.TorPage_ConnectionClosed)
                    }
                }
            }
        })

        viewModel.transactionOrderingLiveData.observe(this, Observer { ordering ->
            transactionsOrderSetting.showDropdownValue(getSortingLocalized(ordering))
        })

        viewModel.showAppRestartAlertForTor.observe(this, Observer { checked ->
            showAppRestartAlert(checked)
        })

        viewModel.showNotificationsNotEnabledAlert.observe(this, Observer {
            showNotificationsNotEnabledAlert()
        })

        viewModel.showTorPrerequisitesAlert.observe(this, Observer {
            showTorPrerequisitesAlert()
        })

        viewModel.showTransactionsSortingSelectorDialog.observe(this, Observer { (items, selected) ->
            BottomSheetSelectorDialog.show(
                    supportFragmentManager,
                    getString(R.string.SettingsPrivacy_Transactions),
                    getString(R.string.SettingsPrivacy_TransactionsSettingText),
                    R.drawable.ic_transactions,
                    items.map { getSortingInfo(it) },
                    items.indexOf(selected),
                    object : OnItemSelectedListener {
                        override fun onItemSelected(position: Int) {
                            viewModel.delegate.onSelectTransactionSorting(items[position])
                        }
                    }
            )
        })

        // IRouter
        viewModel.restartApp.observe(this, Observer {
            restartApp()
        })
    }

    private fun createCommunicationSettingsView(doCreate: Boolean){

        if(doCreate) {
            communicationSettingsAdapter = PrivacySettingsAdapter(viewModel.delegate)
            communicationSettingsRecyclerview.adapter = communicationSettingsAdapter

            viewModel.communicationSettingsViewItems.observe(this, Observer {
                communicationSettingsAdapter.items = it
                communicationSettingsAdapter.notifyDataSetChanged()
            })

            viewModel.showCommunicationSelectorDialog.observe(this, Observer { (items, selected, coin) ->
                BottomSheetSelectorDialog.show(
                        supportFragmentManager,
                        getString(R.string.SettingsPrivacy_CommunicationSettingsTitle),
                        coin.title,
                        LayoutHelper.getCoinDrawableResource(this, coin.code),
                        items.map { getCommunicationModeInfo(it) },
                        items.indexOf(selected),
                        object : OnItemSelectedListener {
                            override fun onItemSelected(position: Int) {
                                viewModel.delegate.onSelectSetting(position)
                            }
                        }
                )
            })

            viewModel.showCommunicationModeChangeAlert.observe(this, Observer { (coin, communicationMode) ->
                ConfirmationDialog.show(
                        title = getString(R.string.BlockchainSettings_CommunicationModeChangeAlert_Title),
                        subtitle = communicationMode.title,
                        contentText = getString(R.string.Tor_PrerequisitesAlert_Content),
                        actionButtonTitle = getString(R.string.Button_Change),
                        activity = this,
                        listener = object : ConfirmationDialog.Listener {
                            override fun onActionButtonClick() {
                                viewModel.delegate.proceedWithCommunicationModeChange(coin, communicationMode)
                            }

                            override fun onCancelButtonClick() {
                                setTorSwitch(false)
                                viewModel.delegate.onApplyTorPrerequisites(false)
                            }
                        }
                )
            })
        }

        communicationSettingsTitle.visibility = if(doCreate) View.VISIBLE else View.GONE
        communicationSettingsDescription.visibility = if(doCreate) View.VISIBLE else View.GONE
        communicationSettingsRecyclerview.visibility = if(doCreate) View.VISIBLE else View.GONE
    }

    private fun createWalletRestoreSettingsView(doCreate: Boolean){

        if(doCreate) {

            walletRestoreSettingsAdapter = PrivacySettingsAdapter(viewModel.delegate)
            walletRestoreSettingsRecyclerview.adapter = walletRestoreSettingsAdapter

            viewModel.restoreWalletSettingsViewItems.observe(this, Observer {
                walletRestoreSettingsAdapter.items = it
                walletRestoreSettingsAdapter.notifyDataSetChanged()
            })

            viewModel.showSyncModeSelectorDialog.observe(this, Observer { (items, selected, coin) ->
                BottomSheetSelectorDialog.show(
                        supportFragmentManager,
                        getString(R.string.BlockchainSettings_SyncModeChangeAlert_Title),
                        coin.title,
                        LayoutHelper.getCoinDrawableResource(this, coin.code),
                        items.map { getSyncModeInfo(it) },
                        items.indexOf(selected),
                        object : OnItemSelectedListener {
                            override fun onItemSelected(position: Int) {
                                viewModel.delegate.onSelectSetting(position)
                            }
                        },
                        warning = getString(R.string.BlockchainSettings_SyncModeChangeAlert_Content, coin.title)
                )
            })
        }

        walletRestoreTitle.visibility = if(doCreate) View.VISIBLE else View.GONE
        walletRestoreSettingsDescription.visibility = if(doCreate) View.VISIBLE else View.GONE
        walletRestoreSettingsRecyclerview.visibility = if(doCreate) View.VISIBLE else View.GONE
    }

    private fun getSortingLocalized(sortingType: TransactionDataSortingType): String{
        return when(sortingType) {
            TransactionDataSortingType.Shuffle -> getString(R.string.SettingsSecurity_SortingShuffle)
            TransactionDataSortingType.Bip69 -> getString(R.string.SettingsSecurity_SortingBip69)
        }
    }

    private fun getSortingInfo(sortingType: TransactionDataSortingType): Pair<String, String> {
        return when(sortingType) {
            TransactionDataSortingType.Shuffle -> {
                Pair(getString(R.string.SettingsSecurity_SortingShuffle), getString(R.string.SettingsSecurity_SortingShuffleDescription))
            }
            TransactionDataSortingType.Bip69 -> {
                Pair(getString(R.string.SettingsSecurity_SortingBip69), getString(R.string.SettingsSecurity_SortingBip69Description))
            }
        }
    }

    private fun getSyncModeInfo(syncMode: SyncMode): Pair<String, String> {
        return when(syncMode) {
            SyncMode.Fast -> Pair(getString(R.string.SettingsSecurity_SyncModeAPI), getString(R.string.SettingsSecurity_SyncModeAPIDescription))
            SyncMode.Slow -> Pair(getString(R.string.SettingsSecurity_SyncModeBlockchain), getString(R.string.SettingsSecurity_SyncModeBlockchainDescription))
            SyncMode.New -> throw Exception("Unsupported syncMode: $syncMode")
        }
    }

    private fun getCommunicationModeInfo(communicationMode: CommunicationMode): Pair<String, String> {
        return when(communicationMode) {
            CommunicationMode.Infura -> Pair(communicationMode.title, "infura.io")
            CommunicationMode.Incubed -> Pair(communicationMode.title, "slock.it")
            else -> throw Exception("Unsupported syncMode: $communicationMode")
        }
    }

    private fun setTorSwitch(checked: Boolean) {
        torConnectionSwitch.setOnCheckedChangeListener(null)
        torConnectionSwitch.isChecked = checked
        torConnectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.delegate.didSwitchTorEnabled(isChecked)
        }
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
                        viewModel.delegate.onApplyTorPrerequisites(!checked)
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

    private fun showTorPrerequisitesAlert() {
        AlertDialogFragment.newInstance(
                descriptionString = getString(R.string.Tor_PrerequisitesAlert_Content),
                buttonText = R.string.Button_Ok,
                cancelable = true,
                listener = object : AlertDialogFragment.Listener {
                    override fun onButtonClick() {
                        viewModel.delegate.updateTorState(true)
                    }

                    override fun onCancel() {
                    }
                }).show(supportFragmentManager, "alert_dialog")
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
