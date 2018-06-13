package bitcoin.wallet.modules.addWallet

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import bitcoin.wallet.R
import bitcoin.wallet.modules.backupWords.BackupWordsModule
import bitcoin.wallet.modules.restoreWallet.RestoreWalletModule
import kotlinx.android.synthetic.main.activity_add_wallet.*

class AddWalletActivity : AppCompatActivity() {

    private lateinit var viewModel: AddWalletViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_wallet)

        viewModel = ViewModelProviders.of(this).get(AddWalletViewModel::class.java)
        viewModel.init()

        viewModel.openBackupScreenLiveEvent.observe(this, Observer {
            BackupWordsModule.start(this)
            finish()
        })

        viewModel.openRestoreWalletScreenLiveEvent.observe(this, Observer {
            RestoreWalletModule.start(this)
            finish()
        })

        buttonCreate.setOnClickListener {
            viewModel.presenter.createWallet()
        }

        buttonRestore.setOnClickListener {
            viewModel.presenter.restoreWallet()
        }
    }
}
