package io.horizontalsystems.bankwallet.modules.backup

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.pin.PinModule

class BackupActivity : BaseActivity() {

    private lateinit var viewModel: BackupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_backup_words)

        viewModel = ViewModelProviders.of(this).get(BackupViewModel::class.java)
        viewModel.init(BackupPresenter.DismissMode.valueOf(intent.getStringExtra(dismissModeKey)))

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, BackupInfoFragment()).commit()

        }

        viewModel.navigationWordsLiveEvent.observe(this, Observer {

            val transaction = supportFragmentManager.beginTransaction()

            transaction.replace(R.id.fragmentContainer, BackupWordsFragment())
            transaction.addToBackStack(null)

            transaction.commit()

        })

        viewModel.navigationConfirmLiveEvent.observe(this, Observer {

            val transaction = supportFragmentManager.beginTransaction()

            transaction.replace(R.id.fragmentContainer, BackupConfirmFragment())
            transaction.addToBackStack(null)

            transaction.commit()

        })

        viewModel.navigateBackLiveEvent.observe(this, Observer {
            hideSoftKeyboard()
            supportFragmentManager.popBackStack()
        })

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
