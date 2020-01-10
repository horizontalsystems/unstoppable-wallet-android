package io.horizontalsystems.bankwallet.modules.settings.managekeys.views

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.PresentationMode
import io.horizontalsystems.bankwallet.modules.backup.BackupModule
import io.horizontalsystems.bankwallet.modules.createwallet.CreateWalletModule
import io.horizontalsystems.bankwallet.modules.restore.restorecoins.RestoreCoinsModule
import io.horizontalsystems.bankwallet.modules.settings.managekeys.ManageKeysViewModel
import io.horizontalsystems.bankwallet.ui.dialogs.ManageKeysDeleteAlert
import io.horizontalsystems.bankwallet.ui.dialogs.ManageKeysDialog
import io.horizontalsystems.bankwallet.ui.dialogs.ManageKeysDialog.ManageAction
import kotlinx.android.synthetic.main.activity_manage_keys.*

class ManageKeysActivity : BaseActivity(), ManageKeysDialog.Listener {

    private lateinit var viewModel: ManageKeysViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(ManageKeysViewModel::class.java)
        viewModel.init()

        setContentView(R.layout.activity_manage_keys)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val adapter = ManageKeysAdapter(viewModel)
        recyclerView.adapter = adapter

        viewModel.confirmUnlinkEvent.observe(this, Observer { item ->
            item.account?.let { account ->

                val confirmationList = listOf(
                        getString(R.string.ManageKeys_Delete_ConfirmationRemove, getString(item.predefinedAccountType.title)),
                        getString(R.string.ManageKeys_Delete_ConfirmationDisable, getString(item.predefinedAccountType.coinCodes)),
                        getString(R.string.ManageKeys_Delete_ConfirmationLose)
                )

                val confirmListener = object : ManageKeysDeleteAlert.Listener {
                    override fun onConfirmationSuccess() {
                        viewModel.delegate.onConfirmUnlink(account.id)
                    }
                }

                ManageKeysDeleteAlert.show(this, getString(item.predefinedAccountType.title), confirmationList, confirmListener)
            }
        })

        viewModel.confirmBackupEvent.observe(this, Observer {
            val title = getString(R.string.ManageKeys_Delete_Alert_Title)
            val subtitle = getString(it.predefinedAccountType.title)
            val description = getString(R.string.ManageKeys_Delete_Alert)
            ManageKeysDialog.show(title, subtitle, description, this, this, ManageAction.BACKUP)
        })

        viewModel.showBackupModule.observe(this, Observer { (account, predefinedAccountType) ->
            BackupModule.start(this, account, getString(predefinedAccountType.coinCodes))
        })

        viewModel.showCreateWalletLiveEvent.observe(this, Observer { predefinedAccountType ->
            CreateWalletModule.startInApp(this, predefinedAccountType)
        })

        viewModel.showCoinRestoreLiveEvent.observe(this, Observer { predefinedAccountType->
            RestoreCoinsModule.start(this, predefinedAccountType, PresentationMode.InApp)
        })

        viewModel.showItemsEvent.observe(this, Observer { list ->
            adapter.items = list
            adapter.notifyDataSetChanged()
        })

        viewModel.closeLiveEvent.observe(this, Observer {
            finish()
        })
    }

    //  ManageKeysDialog Listener

    override fun onClickBackupKey() {
        viewModel.delegate.onConfirmBackup()
    }
}
