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
import cash.p.terminal.tangem.ui.HardwareWalletOnboardingUIState
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.headline1_leah
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
internal fun CreateBackupScreen(
    uiState: HardwareWalletOnboardingUIState,
    onCreateBackupClick: () -> Unit,
    onGoToFinalPageClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        val title = if(uiState.backupCards.isEmpty()) {
            stringResource(R.string.np_backup_devices)
        } else {
            stringResource(R.string.more_backup_devices)
        }
        headline1_leah(title)
        val description = if(uiState.backupCards.isEmpty()) {
            stringResource(R.string.onboarding_subtitle_no_backup_cards)
        } else {
            stringResource(R.string.onboarding_subtitle_more_backup_cards)
        }
        subhead1_grey(
            text = description,
            modifier = Modifier.padding(
                top = 32.dp,
                start = 32.dp,
                end = 32.dp
            )
        )
        Spacer(Modifier.weight(1f))
        if(uiState.backupCards.isEmpty()) {
            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.add_backup_card),
                onClick = onCreateBackupClick
            )
        } else {
            ButtonPrimaryDefault(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                title = stringResource(R.string.add_more_backup_cards),
                onClick = onCreateBackupClick
            )
            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.finalize_backup),
                onClick = onGoToFinalPageClick
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun CreateBackupScreenPreview() {
    ComposeAppTheme {
        CreateBackupScreen(
            onCreateBackupClick = {},
            onGoToFinalPageClick = {},
            uiState = HardwareWalletOnboardingUIState(
                backupCards = emptyList()
            )
        )
    }
}