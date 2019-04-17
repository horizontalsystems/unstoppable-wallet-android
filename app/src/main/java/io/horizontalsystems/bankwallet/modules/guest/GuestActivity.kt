package io.horizontalsystems.bankwallet.modules.guest

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.backup.BackupModule
import io.horizontalsystems.bankwallet.modules.backup.BackupPresenter
import io.horizontalsystems.bankwallet.modules.restore.RestoreModule
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.activity_add_wallet.*

class GuestActivity : BaseActivity() {

    private lateinit var viewModel: GuestViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar()

        setContentView(R.layout.activity_add_wallet)

        viewModel = ViewModelProviders.of(this).get(GuestViewModel::class.java)
        viewModel.init()

        viewModel.openBackupScreenLiveEvent.observe(this, Observer {
            BackupModule.start(this, BackupPresenter.DismissMode.SET_PIN)
            finish()
        })

        viewModel.openRestoreWalletScreenLiveEvent.observe(this, Observer {
            RestoreModule.start(this)
        })

        viewModel.showErrorDialog.observe(this, Observer {
            HudHelper.showErrorMessage(R.string.Error)
        })

        viewModel.appVersionLiveData.observe(this, Observer { appVersion ->
            appVersion?.let { textVersion.text = getString(R.string.Guest_Version, it) }
        })

        buttonCreate.setOnSingleClickListener {
            viewModel.delegate.createWalletDidClick()
        }

        buttonRestore.setOnSingleClickListener {
            viewModel.delegate.restoreWalletDidClick()
        }

        viewModel.keyStoreSafeExecute.observe(this, Observer { triple ->
            triple?.let {
                val (action, onSuccess, onFailure) = it
                safeExecuteWithKeystore(action, onSuccess, onFailure)
            }
        })
    }

}
