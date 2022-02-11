package io.horizontalsystems.bankwallet.modules.walletconnect.session.ui

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
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault

@Composable
fun WCSessionError(
    error: String,
    navController: NavController
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
                text = error,
                style = ComposeAppTheme.typography.subhead2,
                color = ComposeAppTheme.colors.grey,
            )
        }
        ButtonPrimaryDefault(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
                .align(Alignment.BottomCenter),
            title = stringResource(R.string.Button_Close),
            onClick = { navController.popBackStack() }
        )
    }
}

@Preview
@Composable
fun PreviewWCSessionsEmpty() {
    val context = LocalContext.current

    ComposeAppTheme {
        WCSessionError("Error text", NavController(context))
    }
}
