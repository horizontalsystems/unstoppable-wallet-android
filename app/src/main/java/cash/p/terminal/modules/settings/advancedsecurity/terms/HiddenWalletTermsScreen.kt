package cash.p.terminal.modules.settings.advancedsecurity.terms

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.entities.TermItem
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
internal fun HiddenWalletTermsScreen(
    uiState: HiddenWalletTermsUiState,
    onCheckboxToggle: (Int) -> Unit,
    onAgreeClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    ChecklistTermsScreen(
        title = stringResource(R.string.AdvancedSecurity_Terms_Title),
        terms = uiState.terms,
        buttonTitle = if (uiState.agreeEnabled) {
            stringResource(R.string.AdvancedSecurity_CreateHiddenWallet)
        } else {
            stringResource(R.string.Button_IAgree)
        },
        buttonEnabled = uiState.agreeEnabled,
        onCheckboxToggle = onCheckboxToggle,
        onAgreeClick = onAgreeClick,
        onNavigateBack = onNavigateBack,
        warningContent = {
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.AdvancedSecurity_Terms_Warning)
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun HiddenWalletTermsScreenPreview() {
    val termTitles = LocalContext.current.resources.getStringArray(R.array.AdvancedSecurity_Terms_Checkboxes)
    val terms = termTitles.mapIndexed { index, title ->
        TermItem(
            id = index,
            title = title,
            checked = false
        )
    }
    ComposeAppTheme {
        HiddenWalletTermsScreen(
            uiState = HiddenWalletTermsUiState(
                terms = terms,
                agreeEnabled = false
            ),
            onCheckboxToggle = {},
            onAgreeClick = {},
            onNavigateBack = {}
        )
    }
}
