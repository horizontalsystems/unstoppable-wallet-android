package io.horizontalsystems.bankwallet.modules.backup.words

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
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

        viewModel = ViewModelProviders.of(this).get(BackupWordsViewModel::class.java)
        viewModel.init(intent.getStringExtra(ACCOUNT_ID_KEY), intent.getStringExtra(WORDS_KEY).split(", "))

        if (savedInstanceState == null) {
            viewModel.delegate.viewDidLoad()
        }

        buttonBack.setOnSingleClickListener { viewModel.delegate.onBackClick() }
        buttonNext.setOnSingleClickListener { viewModel.delegate.onNextClick() }

        viewModel.loadPageLiveEvent.observe(this, Observer { page ->
            page?.let {
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
            }
        })

        viewModel.startPinModuleEvent.observe(this, Observer {
//            PinModule.startForUnlock(this)
        })

        viewModel.notifyBackedUpEvent.observe(this, Observer { accountId ->
            accountId?.let {
                val intent = Intent().apply {
                    putExtra(ACCOUNT_ID_KEY, accountId)
                }
                setResult(RESULT_OK, intent)
                finish()
            }
        })

        viewModel.closeLiveEvent.observe(this, Observer {
            finish()
        })

    }

    override fun onBackPressed() {
        viewModel.delegate.onBackClick()
    }

    companion object {
        const val ACCOUNT_ID_KEY = "accountId"
        const val WORDS_KEY = "words"

        fun start(context: AppCompatActivity, words: List<String>, accountId: String) {
            val intent = Intent(context, BackupWordsActivity::class.java).apply {
                putExtra(ACCOUNT_ID_KEY, accountId)
                putExtra(WORDS_KEY, words.joinToString())
            }

            context.startActivityForResult(intent, ModuleCode.BACKUP_WORDS)
        }
    }
}
