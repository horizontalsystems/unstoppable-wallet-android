package bitcoin.wallet.modules.guest

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import bitcoin.wallet.R
import bitcoin.wallet.modules.backup.BackupModule
import bitcoin.wallet.modules.backup.BackupPresenter
import bitcoin.wallet.modules.restore.RestoreModule
import kotlinx.android.synthetic.main.activity_add_wallet.*

class GuestActivity : AppCompatActivity() {

    private lateinit var viewModel: GuestViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_wallet)

        viewModel = ViewModelProviders.of(this).get(GuestViewModel::class.java)
        viewModel.init()

        viewModel.openBackupScreenLiveEvent.observe(this, Observer {
            BackupModule.start(this, BackupPresenter.DismissMode.TO_MAIN)
            finish()
        })

        viewModel.openRestoreWalletScreenLiveEvent.observe(this, Observer {
            RestoreModule.start(this)
        })

        buttonCreate.setOnClickListener {
            viewModel.delegate.createWalletDidClick()
        }

        buttonRestore.setOnClickListener {
            viewModel.delegate.restoreWalletDidClick()
        }
    }
}
