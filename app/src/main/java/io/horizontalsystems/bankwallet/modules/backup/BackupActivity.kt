package io.horizontalsystems.bankwallet.modules.backup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import kotlinx.android.synthetic.main.activity_backup_words.*

class BackupActivity : BaseActivity() {

    private lateinit var viewModel: BackupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_backup_words)

        viewModel = ViewModelProviders.of(this).get(BackupViewModel::class.java)
        viewModel.init(intent.getStringExtra(accountIdKey))

        if (savedInstanceState == null) {
            viewModel.delegate.viewDidLoad()
        }

        buttonBack.setOnSingleClickListener { viewModel.delegate.onBackClick() }
        buttonNext.setOnSingleClickListener { viewModel.delegate.onNextClick() }

        viewModel.closeLiveEvent.observe(this, Observer {
            finish()
        })

        viewModel.loadPageLiveEvent.observe(this, Observer { page ->
            page?.let {
                val fragment = when (page) {
                    0 -> BackupInfoFragment()
                    1 -> BackupWordsFragment()
                    else -> BackupConfirmFragment()
                }

                supportFragmentManager.beginTransaction().apply {
                    replace(R.id.fragmentContainer, fragment)
                    addToBackStack(null)
                    commit()
                }

                // set buttons
                when (page) {
                    0 -> {
                        buttonBack.setText(R.string.Backup_Intro_Later)
                        buttonNext.setText(R.string.Backup_Intro_BackupNow)
                    }
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

    }

    override fun onBackPressed() {
        viewModel.delegate.onBackClick()
    }

    companion object {
        private const val accountIdKey = "AccountIdKey"

        fun start(context: Context, accountId: String) {
            try {
                val intent = Intent(context, BackupActivity::class.java).apply {
                    putExtra(accountIdKey, accountId)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
