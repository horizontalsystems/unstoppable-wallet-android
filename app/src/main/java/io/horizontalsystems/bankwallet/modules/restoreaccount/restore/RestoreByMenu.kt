package io.horizontalsystems.bankwallet.modules.restoreaccount.restore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun RestoreByMenu(
    viewModel: RestoreViewModel
) {
    ByMenu(
        menuTitle = stringResource(R.string.Restore_By),
        menuValue = stringResource(viewModel.restoreOption.titleRes),
        selectorDialogTitle = stringResource(R.string.Restore_RestoreBy),
        selectorItems = viewModel.restoreOptions.map {
            TabItem(
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
    selectorItems: List<TabItem<T>>,
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

    CellSingleLineLawrenceSection {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    showSelectorDialog = true
                }
                .padding(horizontal = 16.dp)
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
    }
}
