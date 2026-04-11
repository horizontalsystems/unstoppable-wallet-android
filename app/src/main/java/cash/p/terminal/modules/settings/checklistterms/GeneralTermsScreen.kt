package cash.p.terminal.modules.settings.checklistterms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.TermsList
import cash.p.terminal.ui_compose.entities.TermItem
import cash.p.terminal.ui_compose.theme.ComposeAppTheme


@Composable
internal fun GeneralTermsContent(
    termsStrings: List<String>,
    title: String,
    confirmButtonText: String,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    val checkedItems = remember(termsStrings) {
        mutableStateMapOf<Int, Boolean>().apply {
            termsStrings.forEachIndexed { index, _ ->
                this[index] = false
            }
        }
    }

    val allItemsChecked by remember {
        derivedStateOf { checkedItems.values.all { it } }
    }
    val terms by remember {
        derivedStateOf {
            termsStrings.mapIndexed { index, item ->
                val (title, description) = item.split("\n", limit = 2).let { parts ->
                    parts[0] to parts.getOrNull(1)
                }
                TermItem(index, title, description, checkedItems[index] == true)
            }
        }
    }

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = title,
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close_24,
                        onClick = onClose
                    )
                )
            )
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            TermsList(
                terms = terms,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                onItemClicked = { id ->
                    checkedItems[id] = !(checkedItems[id] ?: false)
                }
            )
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
                termsStrings = terms,
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