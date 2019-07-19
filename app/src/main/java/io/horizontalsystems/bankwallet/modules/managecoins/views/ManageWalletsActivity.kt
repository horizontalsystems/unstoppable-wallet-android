package io.horizontalsystems.bankwallet.modules.managecoins.views

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.modules.managecoins.ManageWalletsViewModel
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsModule
import io.horizontalsystems.bankwallet.modules.settings.managekeys.ManageKeysModule
import io.horizontalsystems.bankwallet.ui.dialogs.ManageKeysDialog
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import kotlinx.android.synthetic.main.activity_manage_coins.*

class ManageWalletsActivity : BaseActivity() {

    private lateinit var viewModel: ManageWalletsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_coins)

        viewModel = ViewModelProviders.of(this).get(ManageWalletsViewModel::class.java)
        viewModel.init()

        val adapter = ManageWalletsAdapter(viewModel.delegate)
        recyclerView.adapter = adapter

        shadowlessToolbar.bind(
                title = getString(R.string.ManageCoins_title),
                leftBtnItem = TopMenuItem(R.drawable.back) { onBackPressed() },
                rightBtnItem = TopMenuItem(R.drawable.checkmark_orange) { viewModel.delegate.saveChanges() }
        )

        viewModel.coinsLoadedLiveEvent.observe(this, Observer {
            adapter.notifyDataSetChanged()
        })

        viewModel.showRestoreKeyDialog.observe(this, Observer { coin ->
            ManageKeysDialog.showWithoutCreateOption(this, coin, ManageKeysDialogListener())
        })

        viewModel.showCreateAndRestoreKeyDialog.observe(this, Observer { coin ->
            ManageKeysDialog.show(this, coin, ManageKeysDialogListener())
        })

        viewModel.openRestoreWordsModule.observe(this, Observer {
            RestoreWordsModule.startForResult(this, ModuleCode.RESTORE_WORDS)
        })

        viewModel.startManageKeysLiveEvent.observe(this, Observer {
            ManageKeysModule.start(this)
        })

        viewModel.closeLiveDate.observe(this, Observer {
            finish()
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ModuleCode.RESTORE_WORDS && data != null && resultCode == RESULT_OK) {
            val syncMode = data.getParcelableExtra<SyncMode>("syncMode")
            val accountType = data.getParcelableExtra<AccountType>("accountType")

            viewModel.delegate.onRestore(accountType, syncMode)
        }
    }

    inner class ManageKeysDialogListener : ManageKeysDialog.Listener {
        override fun onClickCreateKey() {
            viewModel.delegate.onClickCreateKey()
        }

        override fun onClickRestoreKey() {
            viewModel.delegate.onClickRestoreKey()
        }

        override fun onCancel() {
            viewModel.delegate.onClickCancel()
        }
    }
}
