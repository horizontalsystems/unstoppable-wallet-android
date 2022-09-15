package io.horizontalsystems.bankwallet.modules.tor

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.launcher.LaunchModule
import io.horizontalsystems.bankwallet.modules.settings.security.tor.TorStatus
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.HSCircularProgressIndicator
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import kotlin.system.exitProcess

class TorConnectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TorConnectionScreen(
                closeScreen = {
                    finish()
                },
                restartApp = {
                    finishAffinity()
                    LaunchModule.start(this)
                    exitProcess(0)
                },
            )
        }
    }

    override fun onBackPressed() {
        //do nothing
    }

}

@Composable
private fun TorConnectionScreen(
    closeScreen: () -> Unit,
    restartApp: () -> Unit,
    viewModel: TorConnectionViewModel = viewModel(factory = TorConnectionModule.Factory())
) {
    if (viewModel.closeView) {
        viewModel.viewClosed()
        closeScreen.invoke()
    }

    if (viewModel.restartApp) {
        viewModel.restartAppCalled()
        restartApp.invoke()
    }

    val textRes = if (viewModel.torStatus == TorStatus.Failed) {
        R.string.Tor_Status_Error
    } else {
        R.string.Tor_Status_Starting
    }

    ComposeAppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = ComposeAppTheme.colors.tyler),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.torStatus == TorStatus.Failed) {
                        Image(
                            painter = painterResource(R.drawable.ic_tor_connection_error_24),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        HSCircularProgressIndicator()
                    }
                }

                Spacer(Modifier.height(16.dp))
                subhead1_grey(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
                    textAlign = TextAlign.Center,
                    text = stringResource(textRes)
                )
                Spacer(Modifier.height(40.dp))
                ButtonSecondaryDefault(
                    title = stringResource(R.string.Button_Retry),
                    onClick = { viewModel.restartTor() },
                    enabled = viewModel.torStatus == TorStatus.Failed
                )
                Spacer(Modifier.height(20.dp))
                ButtonSecondaryTransparent(
                    title = stringResource(R.string.Button_Disable) + " Tor",
                    onClick = { viewModel.stopTor() }
                )
            }
        }
    }
}
