package io.horizontalsystems.bankwallet.modules.walletconnect.list.ui

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault

@Composable
fun WCSessionsEmpty(
    qrScannerLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_empty_in_circle_100),
            contentDescription = null
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            modifier = Modifier.padding(horizontal = 48.dp),
            text = stringResource(id = R.string.WalletConnect_NoConnection),
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey,
        )
        Spacer(modifier = Modifier.height(32.dp))
        ButtonSecondaryDefault(
            title = stringResource(id = R.string.WalletConnect_NewConnect),
            onClick = {
                qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context, true))
            },
        )
    }
}

@Preview
@Composable
fun PreviewWCSessionsEmpty() {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    ComposeAppTheme {
        WCSessionsEmpty(launcher)
    }
}
