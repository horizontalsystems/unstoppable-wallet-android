package io.horizontalsystems.bankwallet.modules.tor

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.isInvisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.modules.launcher.LaunchModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import kotlinx.android.synthetic.main.activity_tor_connection.*
import kotlin.system.exitProcess

@SuppressLint("SetTextI18n")
class TorConnectionActivity : AppCompatActivity() {

    private lateinit var presenter: TorStatusPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tor_connection)

        presenter = ViewModelProvider(this, TorStatusModule.Factory()).get(TorStatusPresenter::class.java)
        presenter.viewDidLoad()

        observeView(presenter.view)
        observeRouter(presenter.router)

        txDisableTor.setOnClickListener {
            presenter.disableTor()
        }

        txDisableTor.text = "${getString(R.string.Button_Disable)} Tor"
        setStatus(false, getString(R.string.Tor_Status_Starting))
    }

    override fun onBackPressed() {
        //do nothing
    }

    private fun observeRouter(router: TorStatusRouter) {
        router.closeEvent.observe(this, Observer {
            finish()
        })

        router.restartAppEvent.observe(this, Observer {
            finishAffinity()
            LaunchModule.start(this)
            exitProcess(0)
        })
    }

    private fun observeView(torStatusView: TorStatusView) {
        torStatusView.torConnectionStatus.observe(this, Observer {status ->

            val isError = status == TorStatus.Failed
            val text = if(status == TorStatus.Connecting)
                getString(R.string.Tor_Status_Starting)
            else getString(R.string.Tor_Status_Error)
            setStatus(isError, text)
        })
    }

    private fun setStatus(isError: Boolean, statusText: String) {
        pgTorStatus.isInvisible = isError
        imgTorStatusError.isInvisible = !isError
        txTorStatus.text = statusText

        setRetryButton(isError)
    }

    private fun setRetryButton(enabled: Boolean) {
        btnRetryCompose.setContent {
            ComposeAppTheme {
                ButtonSecondaryDefault(
                    modifier = Modifier.padding(top = 40.dp),
                    title = getString(R.string.Button_Retry),
                    onClick = {
                        presenter.restartTor()
                    },
                    enabled = enabled
                )
            }
        }
    }
}