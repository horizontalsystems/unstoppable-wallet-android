package bitcoin.wallet.modules.backupWords

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import org.grouvi.wallet.modules.dashboard.DashboardModule
import bitcoin.wallet.R

class BackupWordsActivity : AppCompatActivity() {

    private lateinit var viewModel: BackupWordsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_backup_words)

        viewModel = ViewModelProviders.of(this).get(BackupWordsViewModel::class.java)
        viewModel.init(BackupWordsModule.DismissMode.valueOf(intent.getStringExtra("DismissMode")))

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, BackupWordsInfoFragment()).commit()

        }


        viewModel.navigationWordsLiveEvent.observe(this, Observer {

            val transaction = supportFragmentManager.beginTransaction()

            transaction.replace(R.id.fragmentContainer, BackupWordsShowWordsFragment())
            transaction.addToBackStack(null)

            transaction.commit()

        })

        viewModel.navigationConfirmLiveEvent.observe(this, Observer {

            val transaction = supportFragmentManager.beginTransaction()

            transaction.replace(R.id.fragmentContainer, BackupWordsConfirmFragment())
            transaction.addToBackStack(null)

            transaction.commit()

        })

        viewModel.navigateBackLiveEvent.observe(this, Observer {
            supportFragmentManager.popBackStack()
        })

        viewModel.navigateToMainLiveEvent.observe(this, Observer {
            DashboardModule.start(this)
        })

        viewModel.closeLiveEvent.observe(this, Observer {
            finish()
        })


    }
}