package cash.p.terminal.modules.backuplocal.terms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HsCheckbox
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.TextImportantWarning
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.subhead2_leah

@Composable
fun LocalBackupTermsScreen(
    fragmentNavController: NavController,
    navController: NavController,
) {
    var term1Checked by rememberSaveable { mutableStateOf(false) }
    var term2Checked by rememberSaveable { mutableStateOf(false) }

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.LocalBackup_Title),
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = {
                                fragmentNavController.popBackStack()
                            }
                        )
                    )
                )
            }
        ) {
            Column(modifier = Modifier.padding(it)) {
                Column(modifier = Modifier.weight(1f)) {
                    TextImportantWarning(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        text = stringResource(R.string.LocalBackup_TermsWarningText)
                    )
                    VSpacer(24.dp)
                    CellUniversalLawrenceSection(
                        listOf(
                            {
                                LocalBackupTerm(
                                    text = stringResource(R.string.LocalBackup_Term1),
                                    checked = term1Checked,
                                    onCheckedChange = { checked ->
                                        term1Checked = checked
                                    }
                                )
                            }, {
                                LocalBackupTerm(
                                    text = stringResource(R.string.LocalBackup_Term2),
                                    checked = term2Checked,
                                    onCheckedChange = { checked ->
                                        term2Checked = checked
                                    }
                                )
                            }
                        )
                    )
                }
                ButtonsGroupWithShade {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                        title = stringResource(R.string.Button_Continue),
                        enabled = term1Checked && term2Checked,
                        onClick = {
                            navController.navigate("password_page")
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalBackupTerm(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = {
            onCheckedChange.invoke(checked.not())
        }
    ) {
        HsCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Spacer(Modifier.width(16.dp))
        subhead2_leah(text = text)
    }
}
