package io.horizontalsystems.bankwallet.modules.rooteddevice

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey

class RootedDeviceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RootedDeviceScreen(onClose = { finish() })
        }
    }

    override fun onBackPressed() {
        //disable close with back button
    }

}

@Composable
private fun RootedDeviceScreen(
    onClose: () -> Unit,
    viewModel: RootedDeviceViewModel = viewModel(factory = RootedDeviceModule.Factory())
) {
    ComposeAppTheme {
        Column(Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_attention_24),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    subhead2_grey(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
                        textAlign = TextAlign.Center,
                        text = stringResource(R.string.Alert_DeviceIsRootedWarning)
                    )
                }
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    title = stringResource(R.string.RootedDevice_Button_Understand),
                    onClick = {
                        viewModel.ignoreRootedDeviceWarning()
                        onClose.invoke()
                    },
                )
            }
        }
    }
}

@Preview
@Composable
fun Preview_RootedDeviceScreen() {
    ComposeAppTheme {
        RootedDeviceScreen({})
    }
}
