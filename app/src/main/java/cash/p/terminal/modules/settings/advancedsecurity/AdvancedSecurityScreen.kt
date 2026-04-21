package cash.p.terminal.modules.settings.advancedsecurity

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsSwitch
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
internal fun AdvancedSecurityScreen(
    uiState: AdvancedSecurityUiState,
    onCreateHiddenWalletClick: () -> Unit,
    onSecureResetToggle: (Boolean) -> Unit,
    onDeleteContactsToggle: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.advanced_security),
                navigationIcon = {
                    HsBackButton(onClick = onClose)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            CellUniversalLawrenceSection(
                listOf({
                    RowUniversal(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = onCreateHiddenWalletClick
                    ) {
                        body_leah(
                            text = stringResource(R.string.AdvancedSecurity_CreateHiddenWallet),
                            maxLines = 1,
                        )
                        Spacer(Modifier.weight(1f))
                        Image(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.ic_arrow_right),
                            contentDescription = null,
                        )
                    }
                }),
                Modifier.padding(top = 12.dp)
            )
            VSpacer(16.dp)
            CellUniversalLawrenceSection(
                listOf(
                    {
                        RowUniversal(
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            body_leah(
                                text = stringResource(R.string.AdvancedSecurity_SecureReset),
                                maxLines = 1,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            HsSwitch(
                                checked = uiState.isSecureResetPinSet,
                                onCheckedChange = onSecureResetToggle
                            )
                        }
                    },
                    {
                        RowUniversal(
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            body_leah(
                                text = stringResource(R.string.delete_all_contacts),
                                maxLines = 1,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            HsSwitch(
                                checked = uiState.isDeleteContactsPinSet,
                                onCheckedChange = onDeleteContactsToggle
                            )
                        }
                    }
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdvancedSecurityScreenPreview() {
    ComposeAppTheme {
        AdvancedSecurityScreen(
            uiState = AdvancedSecurityUiState(
                isSecureResetPinSet = false,
                isDeleteContactsPinSet = false
            ),
            onCreateHiddenWalletClick = {},
            onSecureResetToggle = {},
            onDeleteContactsToggle = {},
            onClose = {}
        )
    }
}
