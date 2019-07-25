package io.horizontalsystems.bankwallet.modules.settings.managekeys.views

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.backup.BackupModule
import io.horizontalsystems.bankwallet.modules.restore.eos.RestoreEosModule
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsModule
import io.horizontalsystems.bankwallet.modules.settings.managekeys.ManageKeysViewModel
import io.horizontalsystems.bankwallet.ui.dialogs.BottomButtonColor
import io.horizontalsystems.bankwallet.ui.dialogs.BottomConfirmAlert
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import kotlinx.android.synthetic.main.activity_manage_keys.*

class ManageKeysActivity : BaseActivity() {

    private lateinit var viewModel: ManageKeysViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(ManageKeysViewModel::class.java)
        viewModel.init()

        setContentView(R.layout.activity_manage_keys)
        shadowlessToolbar.bind(getString(R.string.ManageKeys_Title), TopMenuItem(R.drawable.back) { onBackPressed() })

        val adapter = ManageKeysAdapter(viewModel)
        recyclerView.adapter = adapter

        viewModel.confirmUnlinkEvent.observe(this, Observer { account ->
            account?.let {
                val confirmationList = mutableListOf(
                        R.string.ManageKeys_Unlink_ConfirmationRemove,
                        R.string.ManageKeys_Unlink_ConfirmationDisable
                )

                val confirmListener = object : BottomConfirmAlert.Listener {
                    override fun onConfirmationSuccess() {
                        viewModel.delegate.onClickUnlink(account.id)
                    }
                }

                BottomConfirmAlert.show(this, confirmationList, confirmListener, BottomButtonColor.RED)
            }
        })

        viewModel.startBackupModuleLiveEvent.observe(this, Observer { account ->
            account?.let { BackupModule.start(this, account) }
        })

        viewModel.startRestoreWordsLiveEvent.observe(this, Observer {
            RestoreWordsModule.startForResult(this, ModuleCode.RESTORE_WORDS)
        })

        viewModel.startRestoreEosLiveEvent.observe(this, Observer {
            RestoreEosModule.startForResult(this, ModuleCode.RESTORE_EOS)
        })

        viewModel.closeLiveEvent.observe(this, Observer {
            finish()
        })

        viewModel.showItemsEvent.observe(this, Observer { list ->
            list?.let {
                adapter.items = it
                adapter.notifyDataSetChanged()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data == null || resultCode != RESULT_OK)
            return

        val accountType = data.getParcelableExtra<AccountType>("accountType")

        when (requestCode) {
            ModuleCode.RESTORE_WORDS -> {
                viewModel.delegate.onRestore(accountType, data.getParcelableExtra("syncMode"))
            }
            ModuleCode.RESTORE_EOS -> {
                viewModel.delegate.onRestore(accountType)
            }
        }
    }
}
