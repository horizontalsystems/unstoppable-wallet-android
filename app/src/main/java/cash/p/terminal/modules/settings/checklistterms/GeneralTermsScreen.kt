package cash.p.terminal.modules.settings.checklistterms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cash.p.terminal.R
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HsCheckbox
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme


@Composable
internal fun GeneralTermsContent(
    terms: List<String>,
    title: String,
    confirmButtonText: String,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    val checkedItems = remember { mutableStateMapOf<Int, Boolean>() }

    LaunchedEffect(terms) {
        terms.forEachIndexed { index, _ ->
            checkedItems[index] = false
        }
    }

    val allItemsChecked = checkedItems.values.all { it }

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = title,
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = onClose
                    )
                )
            )
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                CellUniversalLawrenceSection(
                    items = terms.mapIndexed { index, description ->
                        TermsItem(index, description, checkedItems[index] ?: false)
                    },
                    showFrame = true
                ) { item ->
                    RowUniversal(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment =  Alignment.Top,
                        onClick = { checkedItems[item.index] = !checkedItems[item.index]!! }
                    ) {
                        HsCheckbox(
                            checked = item.checked,
                            enabled = true,
                            modifier = Modifier.padding(top = 8.dp),
                            onCheckedChange = { checked ->
                                checkedItems[item.index] = checked
                            },
                        )
                        Spacer(Modifier.width(16.dp))
                        val parts = item.description.split("\n", limit = 2)
                        Column {
                            subhead2_leah(
                                text = parts[0]
                            )
                            if(parts.size > 1) {
                                subhead2_grey(
                                    text = parts[1],
                                     modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(60.dp))
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = confirmButtonText,
                    onClick = onConfirm,
                    enabled = allItemsChecked
                )
            }
        }
    }
}

private data class TermsItem(
    val index: Int,
    val description: String,
    val checked: Boolean
)

@Composable
internal fun GeneralTermsDialog(
    terms: List<String>,
    title: String,
    confirmButtonText: String,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(ComposeAppTheme.colors.tyler)
        ) {
            GeneralTermsContent(
                terms = terms,
                title = title,
                confirmButtonText = confirmButtonText,
                onClose = onClose,
                onConfirm = onConfirm
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_GeneralTermsDialog() {
    ComposeAppTheme {
        GeneralTermsDialog(
            terms = listOf(
                "Terms of Service\nSecond line",
                "Privacy Policy",
                "Cookie Policy"
            ),
            title = "General Terms",
            confirmButtonText = "I Agree",
            onClose = {},
            onConfirm = {}
        )
    }
}