package cash.p.terminal.modules.settings.advancedsecurity.securereset

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.settings.advancedsecurity.terms.ChecklistTermsScreen
import cash.p.terminal.ui_compose.components.TextImportantError
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.entities.TermItem
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
internal fun SecureResetTermsScreen(
    uiState: SecureResetTermsUiState,
    onCheckboxToggle: (Int) -> Unit,
    onAgreeClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    ChecklistTermsScreen(
        title = stringResource(R.string.SecureReset_Terms_Title),
        terms = uiState.terms,
        buttonTitle = stringResource(R.string.Button_IAgree),
        buttonEnabled = uiState.agreeEnabled && uiState.allBackedUp,
        onCheckboxToggle = onCheckboxToggle,
        onAgreeClick = onAgreeClick,
        onNavigateBack = onNavigateBack,
        checkboxesEnabled = uiState.allBackedUp,
        warningContent = {
            if (uiState.allBackedUp) {
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.SecureReset_Terms_Warning)
                )
            } else {
                TextImportantError(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.SecureReset_Terms_Warning_NoBackup)
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun SecureResetTermsScreenPreview() {
    val termTitles = LocalContext.current.resources.getStringArray(R.array.SecureReset_Terms_Checkboxes)
    val terms = termTitles.mapIndexed { index, title ->
        TermItem(
            id = index,
            title = title,
            checked = false
        )
    }
    ComposeAppTheme {
        SecureResetTermsScreen(
            uiState = SecureResetTermsUiState(
                terms = terms,
                agreeEnabled = false,
                allBackedUp = true
            ),
            onCheckboxToggle = {},
            onAgreeClick = {},
            onNavigateBack = {}
        )
    }
}
