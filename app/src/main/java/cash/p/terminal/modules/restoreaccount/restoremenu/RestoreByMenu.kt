package cash.p.terminal.modules.restoreaccount.restoremenu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui_compose.components.B2
import io.horizontalsystems.core.CellUniversalLawrenceSection
import io.horizontalsystems.core.RowUniversal
import cash.p.terminal.ui.compose.components.SelectorDialogCompose
import cash.p.terminal.ui.compose.components.SelectorItem
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun RestoreByMenu(
    viewModel: RestoreMenuViewModel
) {
    ByMenu(
        menuTitle = stringResource(R.string.Restore_By),
        menuValue = stringResource(viewModel.restoreOption.titleRes),
        selectorDialogTitle = stringResource(R.string.Restore_RestoreBy),
        selectorItems = viewModel.restoreOptions.map {
            SelectorItem(
                stringResource(it.titleRes),
                it == viewModel.restoreOption,
                it
            )
        },
        onSelectItem = {
            viewModel.onRestoreOptionSelected(it)
        }
    )
}

@Composable
fun <T> ByMenu(
    menuTitle: String,
    menuValue: String,
    selectorDialogTitle: String,
    selectorItems: List<SelectorItem<T>>,
    onSelectItem: (T) -> Unit
) {
    var showSelectorDialog by remember { mutableStateOf(false) }

    if (showSelectorDialog) {
        SelectorDialogCompose(
            title = selectorDialogTitle,
            items = selectorItems,
            onDismissRequest = {
                showSelectorDialog = false
            },
            onSelectItem = onSelectItem
        )
    }

    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = { showSelectorDialog = true }
            ) {
                B2(text = menuTitle)
                Spacer(Modifier.weight(1f))
                Row {
                    subhead1_grey(text = menuValue)
                    Icon(
                        modifier = Modifier.padding(start = 4.dp),
                        painter = painterResource(id = R.drawable.ic_down_arrow_20),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.grey
                    )
                }
            }
        })
}
