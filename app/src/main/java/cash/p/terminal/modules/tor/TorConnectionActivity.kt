package cash.p.terminal.modules.tor

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.modules.launcher.LaunchModule
import cash.p.terminal.modules.settings.security.tor.TorStatus
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.subhead2_grey
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
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(color = ComposeAppTheme.colors.raina),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.torStatus == TorStatus.Failed) {
                        Image(
                            painter = painterResource(R.drawable.ic_tor_connection_error_24),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = ComposeAppTheme.colors.grey,
                            strokeWidth = 4.dp
                        )
                    }
                }

                subhead2_grey(
                    modifier = Modifier.padding(32.dp),
                    textAlign = TextAlign.Center,
                    text = stringResource(textRes)
                )

                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    title = stringResource(R.string.Button_Retry),
                    onClick = { viewModel.restartTor() },
                    enabled = viewModel.torStatus == TorStatus.Failed
                )

                Spacer(Modifier.height(16.dp))

                ButtonPrimaryTransparent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    title = stringResource(R.string.Button_Disable),
                    onClick = { viewModel.stopTor() }
                )
            }
        }
    }
}
