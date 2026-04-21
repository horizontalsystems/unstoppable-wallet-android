package cash.p.terminal.modules.settings.advancedsecurity.terms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.TermsList
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.entities.TermItem
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
internal fun ChecklistTermsScreen(
    title: String,
    terms: List<TermItem>,
    buttonTitle: String,
    buttonEnabled: Boolean,
    onCheckboxToggle: (Int) -> Unit,
    onAgreeClick: () -> Unit,
    onNavigateBack: () -> Unit,
    warningContent: @Composable () -> Unit,
    checkboxesEnabled: Boolean = true,
) {
    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = title,
                navigationIcon = {
                    HsBackButton(onClick = onNavigateBack)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                VSpacer(12.dp)
                warningContent()
                VSpacer(12.dp)
                TermsList(
                    terms = terms,
                    onItemClicked = if (checkboxesEnabled) onCheckboxToggle else { _ -> }
                )
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = buttonTitle,
                    onClick = onAgreeClick,
                    enabled = buttonEnabled
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChecklistTermsScreenPreview() {
    val termTitles = stringArrayResource(R.array.delete_all_contacts_terms_checkboxes)
    val terms = termTitles.mapIndexed { index, title ->
        TermItem(
            id = index,
            title = title,
            checked = index == 0
        )
    }

    ComposeAppTheme {
        ChecklistTermsScreen(
            title = stringResource(R.string.AdvancedSecurity_Terms_Title),
            terms = terms,
            buttonTitle = stringResource(R.string.Button_IAgree),
            buttonEnabled = false,
            onCheckboxToggle = {},
            onAgreeClick = {},
            onNavigateBack = {},
            warningContent = {
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.SecureReset_Terms_Warning)
                )
            }
        )
    }
}
