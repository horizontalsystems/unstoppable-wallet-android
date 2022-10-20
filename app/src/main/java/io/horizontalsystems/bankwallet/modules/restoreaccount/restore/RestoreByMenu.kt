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
    var showRestoreBySelectorDialog by remember { mutableStateOf(false) }

    if (showRestoreBySelectorDialog) {
        SelectorDialogCompose(
            title = stringResource(R.string.Restore_RestoreBy),
            items = viewModel.restoreOptions.map {
                TabItem(
                    stringResource(it.titleRes),
                    it == viewModel.restoreOption,
                    it
                )
            },
            onDismissRequest = {
                showRestoreBySelectorDialog = false
            },
            onSelectItem = {
                viewModel.onRestoreOptionSelected(it)
            }
        )
    }

    CellSingleLineLawrenceSection {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    showRestoreBySelectorDialog = true
                }
                .padding(horizontal = 16.dp)
        ) {
            B2(
                text = stringResource(R.string.Restore_By),
            )
            Spacer(Modifier.weight(1f))
            Row {
                subhead1_grey(
                    text = stringResource(viewModel.restoreOption.titleRes),
                )
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
