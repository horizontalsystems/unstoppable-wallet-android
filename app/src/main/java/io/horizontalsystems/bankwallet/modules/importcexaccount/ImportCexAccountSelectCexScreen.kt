package io.horizontalsystems.bankwallet.modules.importcexaccount

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun ImportCexAccountSelectCexScreen(
    onSelectCex: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit,
) {
    val viewModel =
        viewModel<ImportCexAccountViewModel>(factory = ImportCexAccountViewModel.Factory())
    val cexItems = viewModel.cexItems

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = TranslatableString.ResString(R.string.ImportCexAccount_SelectCex),
                navigationIcon = {
                    HsBackButton(onClick = onNavigateBack)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = onClose
                    )
                )
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            InfoText(text = stringResource(R.string.ImportCexAccount_SelectCexDescription))

            CellUniversalLawrenceSection(cexItems) {
                RowUniversal(
                    onClick = {
                        onSelectCex.invoke(it.id)
                    }
                ) {
                    Image(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .size(32.dp),
                        painter = painterResource(it.icon),
                        contentDescription = null
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        body_leah(
                            text = it.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(1.dp))
                        subhead2_grey(
                            text = it.url,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = null,
                    )
                    HSpacer(width = 16.dp)
                }
            }
        }
    }
}