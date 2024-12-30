package cash.p.terminal.modules.walletconnect.session.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui.compose.components.ListEmptyView

@Composable
fun WCSessionError(
    error: String,
    navController: NavController
) {
    Box(Modifier.fillMaxSize()) {
        ListEmptyView(text = error, icon = R.drawable.ic_stop)
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

    cash.p.terminal.ui_compose.theme.ComposeAppTheme {
        WCSessionError("Error text", NavController(context))
    }
}
