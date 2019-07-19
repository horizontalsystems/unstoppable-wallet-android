package io.horizontalsystems.bankwallet.modules.backup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.modules.backup.words.BackupWordsActivity
import io.horizontalsystems.bankwallet.modules.backup.words.BackupWordsModule
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import kotlinx.android.synthetic.main.activity_backup_words.*

class BackupActivity : BaseActivity() {

    private lateinit var viewModel: BackupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        viewModel = ViewModelProviders.of(this).get(BackupViewModel::class.java)
        viewModel.init(intent.getParcelableExtra(ACCOUNT_KEY))

        buttonBack.setOnSingleClickListener { viewModel.delegate.onClickCancel() }
        buttonNext.setOnSingleClickListener { viewModel.delegate.onClickBackup() }

        viewModel.startPinModuleEvent.observe(this, Observer {
            PinModule.startForUnlock(this)
        })

        viewModel.startBackupEvent.observe(this, Observer { account ->
            account?.let {
                when (account.type) {
                    is AccountType.Mnemonic -> {
                        BackupWordsModule.start(this, account.type.words, account.id)
                    }
                }
            }
        })

        viewModel.closeLiveEvent.observe(this, Observer {
            finish()
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data == null || resultCode != RESULT_OK) return

        when (requestCode) {
            ModuleCode.BACKUP_WORDS -> {
                val accountId = data.getStringExtra(BackupWordsActivity.ACCOUNT_ID_KEY)
                viewModel.delegate.onBackedUp(accountId)
                finish()
            }
        }
    }

    override fun onBackPressed() {
        viewModel.delegate.onClickCancel()
    }

    companion object {
        const val ACCOUNT_KEY = "account_key"

        fun start(context: Context, account: Account) {
            val intent = Intent(context, BackupActivity::class.java).apply {
                putExtra(ACCOUNT_KEY, account)
            }

            context.startActivity(intent)
        }
    }
}
