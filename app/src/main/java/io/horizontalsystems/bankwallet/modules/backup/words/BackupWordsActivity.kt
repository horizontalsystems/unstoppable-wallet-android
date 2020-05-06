package io.horizontalsystems.bankwallet.modules.backup.words

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import kotlinx.android.synthetic.main.activity_backup_words.*

class BackupWordsActivity : BaseActivity() {

    val viewModel by viewModels<BackupWordsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup_words)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val backedUp = intent.getBooleanExtra(ACCOUNT_BACKEDUP, false)
        val backupWords = intent.getStringArrayExtra(WORDS_KEY) ?: arrayOf()

        viewModel.accountTypeTitle = intent.getIntExtra(ACCOUNT_TYPE_TITLE, R.string.AccountType_Unstoppable)
        viewModel.init(backupWords, backedUp)

        if (savedInstanceState == null) {
            viewModel.delegate.viewDidLoad()
        }

        viewModel.loadPageLiveEvent.observe(this, Observer { page ->
            val fragment = when (page) {
                1 -> BackupWordsFragment()
                else -> BackupWordsConfirmFragment()
            }

            supportFragmentManager.beginTransaction().apply {
                replace(R.id.fragmentContainer, fragment)
                addToBackStack(null)
                commit()
            }

            collapsingToolbar.title = when (page) {
                1 -> getString(R.string.Backup_DisplayTitle)
                2 -> getString(R.string.Backup_Confirmation_CheckTitle)
                else -> null
            }
        })

        viewModel.notifyBackedUpEvent.observe(this, Observer {
            setResult(BackupWordsModule.RESULT_BACKUP)
            finish()
        })

        viewModel.notifyClosedEvent.observe(this, Observer {
            setResult(BackupWordsModule.RESULT_SHOW)
            finish()
        })

        viewModel.closeLiveEvent.observe(this, Observer {
            finish()
        })
    }

    override fun onBackPressed() {
        viewModel.delegate.onBackClick()
    }

    companion object {
        const val ACCOUNT_BACKEDUP = "account_backedup"
        const val WORDS_KEY = "words"
        const val ACCOUNT_TYPE_TITLE = "account_type_title"

        fun start(context: AppCompatActivity, words: List<String>, backedUp: Boolean, accountTypeTitle: Int) {
            val intent = Intent(context, BackupWordsActivity::class.java).apply {
                putExtra(WORDS_KEY, words.toTypedArray())
                putExtra(ACCOUNT_BACKEDUP, backedUp)
                putExtra(ACCOUNT_TYPE_TITLE, accountTypeTitle)
            }

            context.startActivityForResult(intent, ModuleCode.BACKUP_WORDS)
        }
    }
}
