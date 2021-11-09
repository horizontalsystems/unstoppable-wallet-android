package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.blockchainLogo
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.TransactionDataSortingType
import io.horizontalsystems.bankwallet.entities.title
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.modules.tor.TorConnectionActivity
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorViewItem
import io.horizontalsystems.bankwallet.ui.extensions.ConfirmationDialog
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.AlertDialogFragment
import kotlinx.android.synthetic.main.fragment_settings_privacy.*
import kotlin.system.exitProcess

class PrivacySettingsFragment :
    BaseFragment(),
    PrivacySettingsTorAdapter.Listener,
    PrivacySettingsTransactionsStructureAdapter.Listener {

    private lateinit var viewModel: PrivacySettingsViewModel
    private lateinit var torControlAdapter: PrivacySettingsTorAdapter
    private lateinit var walletRestoreSettingsAdapter: PrivacySettingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings_privacy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuShowInfo -> {
                    viewModel.delegate.onShowPrivacySettingsInfoClick()
                    true
                }
                else -> false
            }
        }

        viewModel = ViewModelProvider(this).get(PrivacySettingsViewModel::class.java)
        viewModel.init()


        val topDescriptionAdapter = PrivacySettingsHeaderAdapter()
        torControlAdapter = PrivacySettingsTorAdapter(this)
        val transactionsStructureAdapter = PrivacySettingsTransactionsStructureAdapter(this)
        walletRestoreSettingsAdapter = PrivacySettingsAdapter(
            viewModel.delegate,
            getString(R.string.SettingsPrivacy_WalletRestore),
            getString(R.string.SettingsPrivacy_WalletRestoreDescription)
        )

        concatRecyclerView.adapter = ConcatAdapter(
            topDescriptionAdapter,
            torControlAdapter,
            transactionsStructureAdapter,
            walletRestoreSettingsAdapter
        )

        concatRecyclerView.itemAnimator = null


        // IView
        viewModel.showPrivacySettingsInfo.observe(viewLifecycleOwner, Observer {
            findNavController().navigate(
                R.id.privacySettingsFragment_to_privacySettingsInfoFragment,
                null,
                navOptions()
            )
        })

        viewModel.torEnabledLiveData.observe(viewLifecycleOwner, Observer { enabled ->
            torControlAdapter.setTorSwitch(enabled)
        })

        viewModel.setTorConnectionStatus.observe(viewLifecycleOwner, Observer { torStatus ->
            torControlAdapter.bind(torStatus)
        })

        viewModel.transactionOrderingLiveData.observe(viewLifecycleOwner, Observer { ordering ->
            transactionsStructureAdapter.bind(getSortingLocalized(ordering))
        })

        viewModel.showAppRestartAlertForTor.observe(viewLifecycleOwner, Observer { checked ->
            showAppRestartAlert(checked)
        })

        viewModel.showNotificationsNotEnabledAlert.observe(viewLifecycleOwner, Observer {
            showNotificationsNotEnabledAlert()
        })

        viewModel.showTransactionsSortingSelectorDialog.observe(
            viewLifecycleOwner,
            Observer { (items, selected) ->
                BottomSheetSelectorDialog.show(
                    childFragmentManager,
                    getString(R.string.SettingsPrivacy_Transactions),
                    getString(R.string.SettingsPrivacy_TransactionsSettingText),
                    R.drawable.ic_transactions,
                    items.map { getSortingInfo(it) },
                    items.indexOf(selected),
                    onItemSelected = { position ->
                        viewModel.delegate.onSelectTransactionSorting(items[position])
                    }
                )
            })

        viewModel.restoreWalletSettingsViewItems.observe(this, Observer {
            walletRestoreSettingsAdapter.items = it
            walletRestoreSettingsAdapter.notifyDataSetChanged()
        })

        viewModel.showSyncModeSelectorDialog.observe(this, { (items, syncSetting) ->
            BottomSheetSelectorDialog.show(
                childFragmentManager,
                getString(R.string.BlockchainSettings_SyncModeChangeAlert_Title),
                syncSetting.coinType.title,
                syncSetting.coinType.blockchainLogo,
                items.map { getSyncModeInfo(it) },
                items.indexOf(syncSetting.syncMode),
                onItemSelected = { position ->
                    viewModel.delegate.onSelectSyncMode(items[position], syncSetting.coinType)
                },
                warning = getString(
                    R.string.BlockchainSettings_SyncModeChangeAlert_Content,
                    syncSetting.coinType.title
                )
            )
        })

        // IRouter
        viewModel.restartApp.observe(this, Observer {
            restartApp()
        })
    }

    override fun onTorSwitchChecked(checked: Boolean) {
        viewModel.delegate.didSwitchTorEnabled(checked)
    }

    override fun onClick() {
        viewModel.delegate.onTransactionOrderSettingTap()
    }

    private fun getSortingLocalized(sortingType: TransactionDataSortingType): String {
        return when (sortingType) {
            TransactionDataSortingType.Shuffle -> getString(R.string.SettingsSecurity_SortingShuffle)
            TransactionDataSortingType.Bip69 -> getString(R.string.SettingsSecurity_SortingBip69)
        }
    }

    private fun getSortingInfo(sortingType: TransactionDataSortingType): BottomSheetSelectorViewItem {
        return when (sortingType) {
            TransactionDataSortingType.Shuffle -> {
                BottomSheetSelectorViewItem(
                    getString(R.string.SettingsSecurity_SortingShuffle),
                    getString(R.string.SettingsSecurity_SortingShuffleDescription)
                )
            }
            TransactionDataSortingType.Bip69 -> {
                BottomSheetSelectorViewItem(
                    getString(R.string.SettingsSecurity_SortingBip69),
                    getString(R.string.SettingsSecurity_SortingBip69Description)
                )
            }
        }
    }

    private fun getSyncModeInfo(syncMode: SyncMode): BottomSheetSelectorViewItem {
        return when (syncMode) {
            SyncMode.Fast -> BottomSheetSelectorViewItem(
                getString(R.string.SettingsSecurity_SyncModeAPI),
                getString(R.string.SettingsSecurity_SyncModeAPIDescription)
            )
            SyncMode.Slow -> BottomSheetSelectorViewItem(
                getString(R.string.SettingsSecurity_SyncModeBlockchain),
                getString(R.string.SettingsSecurity_SyncModeBlockchainDescription)
            )
            SyncMode.New -> throw Exception("Unsupported syncMode: $syncMode")
        }
    }

    private fun showAppRestartAlert(checked: Boolean) {
        ConfirmationDialog.show(
            icon = R.drawable.ic_tor_connection_24,
            title = getString(R.string.Tor_Title),
            subtitle = getString(R.string.Tor_Connection_Title),
            contentText = getString(R.string.SettingsSecurity_AppRestartWarning),
            actionButtonTitle = getString(R.string.Alert_Restart),
            cancelButtonTitle = null, // Do not show cancel button
            fragmentManager = childFragmentManager,
            listener = object : ConfirmationDialog.Listener {
                override fun onActionButtonClick() {
                    viewModel.delegate.setTorEnabled(checked)
                }

                override fun onCancelButtonClick() {
                    torControlAdapter.setTorSwitch(!checked)
                }
            }
        )
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
                    torControlAdapter.setTorSwitch(false)
                }
            }).show(childFragmentManager, "alert_dialog_notification")
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

}
