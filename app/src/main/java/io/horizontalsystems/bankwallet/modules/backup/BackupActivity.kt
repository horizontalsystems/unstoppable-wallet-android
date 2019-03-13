package io.horizontalsystems.bankwallet.modules.backup

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.ui.dialogs.BottomConfirmAlert
import kotlinx.android.synthetic.main.activity_backup_words.*

class BackupActivity : BaseActivity(), BottomConfirmAlert.Listener {

    private lateinit var viewModel: BackupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar()

        setContentView(R.layout.activity_backup_words)

        viewModel = ViewModelProviders.of(this).get(BackupViewModel::class.java)
        viewModel.init(BackupPresenter.DismissMode.valueOf(intent.getStringExtra(dismissModeKey)))

        if (savedInstanceState == null) {
            viewModel.delegate.viewDidLoad()
        }

        buttonBack.setOnSingleClickListener { viewModel.delegate.onBackClick() }
        buttonNext.setOnSingleClickListener { viewModel.delegate.onNextClick() }

        viewModel.navigateToSetPinLiveEvent.observe(this, Observer {
            PinModule.startForSetPin(this)
        })

        viewModel.closeLiveEvent.observe(this, Observer {
            finish()
        })

        viewModel.keyStoreSafeExecute.observe(this, Observer { triple ->
            triple?.let {
                val (action, onSuccess, onFailure) = it
                safeExecuteWithKeystore(action, onSuccess, onFailure)
            }
        })

        viewModel.showConfirmationCheckDialogLiveEvent.observe(this, Observer {
            val confirmationList = mutableListOf(
                    R.string.Backup_Confirmation_SecretKey,
                    R.string.Backup_Confirmation_DeleteAppWarn,
                    R.string.Backup_Confirmation_LockAppWarn,
                    R.string.Backup_Confirmation_Disclaimer
            )
            BottomConfirmAlert.show(this, confirmationList, this)
        })

        viewModel.loadPageLiveEvent.observe(this, Observer { page ->
            page?.let {
                val fragment = when(it) {
                    0 -> BackupInfoFragment()
                    1 -> BackupWordsFragment()
                    else -> BackupConfirmFragment()
                }
                val transaction = supportFragmentManager.beginTransaction()
                transaction.replace(R.id.fragmentContainer, fragment)
                transaction.addToBackStack(null)
                transaction.commit()

                setButtons(it)
            }
        })

    }

    private fun setButtons(page: Int) {
        when(page) {
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

    override fun onBackPressed() {
        viewModel.delegate.onBackClick()
    }

    override fun onConfirmationSuccess() {
        viewModel.delegate.onTermsConfirm()
    }

    companion object {
        private const val dismissModeKey = "DismissMode"

        fun start(context: Context, dismissMode: BackupPresenter.DismissMode) {
            val intent = Intent(context, BackupActivity::class.java)
            intent.putExtra(dismissModeKey, dismissMode.name)
            context.startActivity(intent)
        }
    }
}
