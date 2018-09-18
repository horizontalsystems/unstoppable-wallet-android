package bitcoin.wallet.modules.backup

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import bitcoin.wallet.BaseActivity
import bitcoin.wallet.R
import bitcoin.wallet.modules.main.MainModule

class BackupActivity : BaseActivity() {

    private lateinit var viewModel: BackupViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_backup_words)

        viewModel = ViewModelProviders.of(this).get(BackupViewModel::class.java)
        viewModel.init(BackupPresenter.DismissMode.valueOf(intent.getStringExtra("DismissMode")))

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
            supportFragmentManager.popBackStack()
        })

        viewModel.navigateToMainLiveEvent.observe(this, Observer {
            MainModule.start(this)
        })

        viewModel.closeLiveEvent.observe(this, Observer {
            finish()
        })


    }
}