package io.horizontalsystems.bankwallet.modules.backup.words

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import kotlinx.android.synthetic.main.activity_backup_words.*

class BackupWordsActivity : BaseActivity() {

    private lateinit var viewModel: BackupWordsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup_words)

        val backedUp = intent.getBooleanExtra(ACCOUNT_BACKEDUP, false)
        val backupWords = intent.getStringArrayExtra(WORDS_KEY) ?: arrayOf()

        viewModel = ViewModelProvider(this).get(BackupWordsViewModel::class.java)
        viewModel.init(backupWords, backedUp)

        if (savedInstanceState == null) {
            viewModel.delegate.viewDidLoad()
        }

        buttonBack.setOnSingleClickListener { viewModel.delegate.onBackClick() }
        buttonNext.setOnSingleClickListener { viewModel.delegate.onNextClick() }

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

            // set buttons
            when (page) {
                1 -> {
                    buttonBack.setText(R.string.Button_Back)
                    buttonNext.setText(R.string.Backup_Button_Next)
                }
                else -> {
                    buttonBack.setText(R.string.Button_Back)
                    buttonNext.setText(R.string.Backup_Button_Submit)
                }
            }

            if (backedUp) {
                buttonBack.visibility = View.GONE
                buttonNext.setText(R.string.Button_Close)
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

        fun start(context: AppCompatActivity, words: List<String>, backedUp: Boolean) {
            val intent = Intent(context, BackupWordsActivity::class.java).apply {
                putExtra(WORDS_KEY, words.toTypedArray())
                putExtra(ACCOUNT_BACKEDUP, backedUp)
            }

            context.startActivityForResult(intent, ModuleCode.BACKUP_WORDS)
        }
    }
}
