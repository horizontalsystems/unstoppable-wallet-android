package io.horizontalsystems.bankwallet.modules.welcome

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.createwallet.CreateWalletModule
import io.horizontalsystems.bankwallet.modules.restore.RestoreModule
import io.horizontalsystems.bankwallet.modules.torpage.TorPageModule
import kotlinx.android.synthetic.main.activity_welcome.*


class WelcomeActivity : BaseActivity() {

    private lateinit var viewModel: WelcomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTransparentStatusBar()

        setContentView(R.layout.activity_welcome)

        viewModel = ViewModelProvider(this).get(WelcomeViewModel::class.java)
        viewModel.init()

        viewModel.openRestoreModule.observe(this, Observer {
            RestoreModule.start(this)
        })

        viewModel.openCreateWalletModule.observe(this, Observer {
            CreateWalletModule.start(this)
        })

        viewModel.appVersionLiveData.observe(this, Observer { appVersion ->
            appVersion?.let {
                var version = it
                if (getString(R.string.is_release) == "false") {
                    version = "$version (${BuildConfig.VERSION_CODE})"
                }
                textVersion.text = getString(R.string.Welcome_Version, version)
            }
        })

        viewModel.openTorPage.observe(this, Observer {
            TorPageModule.start(this)
        })

        buttonCreate.setOnSingleClickListener {
            viewModel.delegate.createWalletDidClick()
        }

        buttonRestore.setOnSingleClickListener {
            viewModel.delegate.restoreWalletDidClick()
        }

        securityCenter.setOnSingleClickListener {
            viewModel.delegate.openTorPage()
        }
    }

}
