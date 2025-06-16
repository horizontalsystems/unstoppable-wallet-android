package cash.p.terminal.tangem.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.strings.R
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.headline1_leah
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
internal fun CreateWalletScreen(
    onCreateWalletClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        headline1_leah(stringResource(R.string.generate_keys_privately))
        subhead1_grey(
            text = stringResource(R.string.onboarding_create_wallet_options_message),
            modifier = Modifier.padding(
                top = 32.dp,
                start = 32.dp,
                end = 32.dp
            )
        )
        Spacer(Modifier.weight(1f))
        ButtonPrimaryYellow(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.create_wallet),
            onClick = onCreateWalletClick
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun CreateWalletScreenPreview() {
    ComposeAppTheme {
        CreateWalletScreen(
            onCreateWalletClick = {}
        )
    }
}