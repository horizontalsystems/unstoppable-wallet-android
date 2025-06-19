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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.strings.R
import cash.p.terminal.tangem.ui.HardwareWalletOnboardingUIState
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.headline1_leah
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import com.tangem.common.CardIdFormatter
import com.tangem.common.core.CardIdDisplayFormat

@Composable
internal fun FinalScreen(
    uiState: HardwareWalletOnboardingUIState,
    onWriteDataClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        val title = if (uiState.cardNumToBackup == -1) {
            stringResource(R.string.common_origin_card)
        } else {
            stringResource(R.string.common_backup_card)
        }
        headline1_leah(title)
        val cardIdFormatter = CardIdFormatter(CardIdDisplayFormat.LastMasked(4))
        subhead1_grey(
            text = stringResource(
                R.string.onboarding_subtitle_scan_backup_card_format,
                cardIdFormatter.getFormattedCardId(
                    if (uiState.cardNumToBackup == -1) {
                        uiState.primaryCardId.orEmpty()
                    } else {
                        cardIdFormatter.getFormattedCardId(uiState.backupCards[uiState.cardNumToBackup].cardId)
                            .orEmpty()
                    }
                ) ?: "****",
            ),
            modifier = Modifier.padding(
                top = 32.dp,
                start = 32.dp,
                end = 32.dp
            ),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        ButtonPrimaryYellow(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.scan_card),
            onClick = onWriteDataClicked
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun FinalScreenPreview() {
    ComposeAppTheme {
        FinalScreen(
            onWriteDataClicked = {},
            uiState = HardwareWalletOnboardingUIState(
                backupCards = emptyList()
            )
        )
    }
}