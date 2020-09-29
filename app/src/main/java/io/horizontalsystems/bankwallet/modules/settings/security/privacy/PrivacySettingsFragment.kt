package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.entities.CommunicationMode
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.TransactionDataSortingType
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.tor.TorConnectionActivity
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorDialog
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.views.AlertDialogFragment
import kotlinx.android.synthetic.main.fragment_settings_privacy.*
import kotlinx.android.synthetic.main.fragment_settings_privacy.toolbar
import kotlin.system.exitProcess

class PrivacySettingsFragment : BaseFragment() {
    private lateinit var viewModel: PrivacySettingsViewModel
    private lateinit var communicationSettingsAdapter: PrivacySettingsAdapter
    private lateinit var walletRestoreSettingsAdapter: PrivacySettingsAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings_privacy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        setHasOptionsMenu(true)

        viewModel = ViewModelProvider(this).get(PrivacySettingsViewModel::class.java)
        viewModel.init()

        createCommunicationSettingsView()

        torConnectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.delegate.didSwitchTorEnabled(isChecked)
        }

        transactionsOrderSetting.setOnClickListener {
            viewModel.delegate.onTransactionOrderSettingTap()
        }

        // IView
        viewModel.showPrivacySettingsInfo.observe(viewLifecycleOwner, Observer { enabled ->
            openPrivacySettingsInfo()
        })

        viewModel.torEnabledLiveData.observe(viewLifecycleOwner, Observer { enabled ->
            setTorSwitch(enabled)
        })

        viewModel.blockchainSettingsVisibilityLiveData.observe(viewLifecycleOwner, Observer { isVisible ->
            createWalletRestoreSettingsView(isVisible)
        })

        viewModel.setTorConnectionStatus.observe(viewLifecycleOwner, Observer { torStatus ->
            torStatus?.let {
                when (torStatus) {
                    TorStatus.Connecting -> {
                        connectionSpinner.isVisible = true
                        controlIcon.imageTintList = getTint(R.color.grey)
                        controlIcon.setImageResource(R.drawable.ic_tor_connected)
                        subtitleText.text = getString(R.string.TorPage_Connecting)
                    }
                    TorStatus.Connected -> {
                        connectionSpinner.isVisible = false
                        controlIcon.imageTintList = getTint(R.color.yellow_d)
                        controlIcon.setImageResource(R.drawable.ic_tor_connected)
                        subtitleText.text = getString(R.string.TorPage_Connected)
                    }
                    TorStatus.Failed -> {
                        connectionSpinner.isVisible = false
                        controlIcon.imageTintList = getTint(R.color.yellow_d)
                        controlIcon.setImageResource(R.drawable.ic_tor_status_error)
                        subtitleText.text = getString(R.string.TorPage_Failed)
                    }
                    TorStatus.Closed -> {
                        connectionSpinner.isVisible = false
                        controlIcon.imageTintList = getTint(R.color.yellow_d)
                        controlIcon.setImageResource(R.drawable.ic_tor)
                        subtitleText.text = getString(R.string.TorPage_ConnectionClosed)
                    }
                }
            }
        })

        viewModel.transactionOrderingLiveData.observe(viewLifecycleOwner, Observer { ordering ->
            transactionsOrderSetting.showDropdownValue(getSortingLocalized(ordering))
        })

        viewModel.showAppRestartAlertForTor.observe(viewLifecycleOwner, Observer { checked ->
            showAppRestartAlert(checked)
        })

        viewModel.showNotificationsNotEnabledAlert.observe(viewLifecycleOwner, Observer {
            showNotificationsNotEnabledAlert()
        })

        viewModel.showTorPrerequisitesAlert.observe(viewLifecycleOwner, Observer {
            showTorPrerequisitesAlert()
        })

        viewModel.showTransactionsSortingSelectorDialog.observe(viewLifecycleOwner, Observer { (items, selected) ->
            BottomSheetSelectorDialog.show(
                    childFragmentManager,
                    getString(R.string.SettingsPrivacy_Transactions),
                    getString(R.string.SettingsPrivacy_TransactionsSettingText),
                    context?.let { ContextCompat.getDrawable(it, R.drawable.ic_transactions) },
                    items.map { getSortingInfo(it) },
                    items.indexOf(selected),
                    onItemSelected = { position ->
                        viewModel.delegate.onSelectTransactionSorting(items[position])
                    }
            )
        })

        // IRouter
        viewModel.restartApp.observe(this, Observer {
            restartApp()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.settings_info_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuShowInfo -> {
                viewModel.delegate.onShowPrivacySettingsInfoClick()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createCommunicationSettingsView() {

        communicationSettingsAdapter = PrivacySettingsAdapter(viewModel.delegate)
        communicationSettingsRecyclerview.adapter = communicationSettingsAdapter

        viewModel.communicationSettingsViewItems.observe(this, Observer {
            communicationSettingsAdapter.items = it
            communicationSettingsAdapter.notifyDataSetChanged()
        })

        viewModel.showCommunicationSelectorDialog.observe(this, Observer { (items, selected, coin) ->
            BottomSheetSelectorDialog.show(
                    childFragmentManager,
                    getString(R.string.SettingsPrivacy_CommunicationSettingsTitle),
                    coin.title,
                    context?.let { AppLayoutHelper.getCoinDrawable(it, coin.code, coin.type) },
                    items.map { getCommunicationModeInfo(it) },
                    items.indexOf(selected),
                    onItemSelected = { position ->
                        viewModel.delegate.onSelectSetting(position)
                    }
            )
        })

        viewModel.showCommunicationModeChangeAlert.observe(this, Observer { (coin, communicationMode) ->
            activity?.let {
                ConfirmationDialog.show(
                        title = getString(R.string.BlockchainSettings_CommunicationModeChangeAlert_Title),
                        subtitle = communicationMode.title,
                        contentText = getString(R.string.Tor_PrerequisitesAlert_Content),
                        actionButtonTitle = getString(R.string.Button_Change),
                        cancelButtonTitle = getString(R.string.Alert_Cancel),
                        activity = it,
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
            }
        })
    }

    private fun createWalletRestoreSettingsView(doCreate: Boolean) {

        if (doCreate) {

            walletRestoreSettingsAdapter = PrivacySettingsAdapter(viewModel.delegate)
            walletRestoreSettingsRecyclerview.adapter = walletRestoreSettingsAdapter

            viewModel.restoreWalletSettingsViewItems.observe(this, Observer {
                walletRestoreSettingsAdapter.items = it
                walletRestoreSettingsAdapter.notifyDataSetChanged()
            })

            viewModel.showSyncModeSelectorDialog.observe(this, Observer { (items, selected, coin) ->
                BottomSheetSelectorDialog.show(
                        childFragmentManager,
                        getString(R.string.BlockchainSettings_SyncModeChangeAlert_Title),
                        coin.title,
                        context?.let { AppLayoutHelper.getCoinDrawable(it, coin.code, coin.type) },
                        items.map { getSyncModeInfo(it) },
                        items.indexOf(selected),
                        onItemSelected = { position ->
                            viewModel.delegate.onSelectSetting(position)
                        },
                        warning = getString(R.string.BlockchainSettings_SyncModeChangeAlert_Content, coin.title)
                )
            })
        }

        walletRestoreTitle.isVisible = doCreate
        walletRestoreSettingsDescription.isVisible = doCreate
        walletRestoreSettingsRecyclerview.isVisible = doCreate
    }

    private fun getSortingLocalized(sortingType: TransactionDataSortingType): String {
        return when (sortingType) {
            TransactionDataSortingType.Shuffle -> getString(R.string.SettingsSecurity_SortingShuffle)
            TransactionDataSortingType.Bip69 -> getString(R.string.SettingsSecurity_SortingBip69)
        }
    }

    private fun getSortingInfo(sortingType: TransactionDataSortingType): Pair<String, String> {
        return when (sortingType) {
            TransactionDataSortingType.Shuffle -> {
                Pair(getString(R.string.SettingsSecurity_SortingShuffle), getString(R.string.SettingsSecurity_SortingShuffleDescription))
            }
            TransactionDataSortingType.Bip69 -> {
                Pair(getString(R.string.SettingsSecurity_SortingBip69), getString(R.string.SettingsSecurity_SortingBip69Description))
            }
        }
    }

    private fun getSyncModeInfo(syncMode: SyncMode): Pair<String, String> {
        return when (syncMode) {
            SyncMode.Fast -> Pair(getString(R.string.SettingsSecurity_SyncModeAPI), getString(R.string.SettingsSecurity_SyncModeAPIDescription))
            SyncMode.Slow -> Pair(getString(R.string.SettingsSecurity_SyncModeBlockchain), getString(R.string.SettingsSecurity_SyncModeBlockchainDescription))
            SyncMode.New -> throw Exception("Unsupported syncMode: $syncMode")
        }
    }

    private fun getCommunicationModeInfo(communicationMode: CommunicationMode): Pair<String, String> {
        return when (communicationMode) {
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
        activity?.let {
            ConfirmationDialog.show(
                    icon = R.drawable.ic_tor,
                    title = getString(R.string.SettingsPrivacy_ConnectionSettingsTitle),
                    subtitle = getString(R.string.SettingsSecurity_EnableTor),
                    contentText = getString(R.string.SettingsSecurity_AppRestartWarning),
                    actionButtonTitle = getString(R.string.Alert_Restart),
                    cancelButtonTitle = null, // Do not show cancel button
                    activity = it,
                    listener = object : ConfirmationDialog.Listener {
                        override fun onActionButtonClick() {
                            viewModel.delegate.setTorEnabled(checked)
                        }

                        override fun onCancelButtonClick() {
                            setTorSwitch(!checked)
                            viewModel.delegate.onApplyTorPrerequisites(!checked)
                        }
                    }
            )
        }
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
                }).show(childFragmentManager, "alert_dialog_notification")
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
                        setTorSwitch(false)
                    }
                }).show(childFragmentManager, "alert_dialog")
    }

    private fun openAppNotificationSettings() {
        val intent = Intent()
        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        intent.putExtra("android.provider.extra.APP_PACKAGE", context?.packageName)
        startActivity(intent)
    }

    private fun restartApp() {
        activity?.let {
            MainModule.startAsNewTask(it)
            if (App.localStorage.torEnabled) {
                val intent = Intent(it, TorConnectionActivity::class.java)
                startActivity(intent)
            }
            exitProcess(0)
        }
    }

    private fun getTint(color: Int) = context?.let { ColorStateList.valueOf(ContextCompat.getColor(it, color)) }

    private fun openPrivacySettingsInfo() {
        activity?.let {
            PrivacySettingsInfoFragment.start(it)
        }
    }
}
