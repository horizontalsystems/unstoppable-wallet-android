package cash.p.terminal.modules.pin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.entities.Account
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HFillSpacer
import cash.p.terminal.ui.compose.components.HeaderText
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.HsCheckbox
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_leah
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_lucian

class SetDuressPinSelectAccountsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SetDuressPinSelectAccountsScreen(navController)
    }
}

@Composable
fun SetDuressPinSelectAccountsScreen(navController: NavController) {
    val viewModel = viewModel<SetDuressPinSelectAccountsViewModel>(factory = SetDuressPinSelectAccountsViewModel.Factory())
    val regularAccounts = viewModel.regularAccounts
    val watchAccounts = viewModel.watchAccounts
    val selected = remember { mutableStateListOf<String>() }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.DuressPinSelectAccounts_Title),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
            )
        },
        bottomBar = {
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.Button_Next),
                    onClick = {
                        navController.slideFromRight(R.id.setDuressPinFragment, SetDuressPinFragment.params(selected))
                    },
                )
            }
        }
    ) { innerPaddings ->
        Column(
            modifier = Modifier
                .padding(innerPaddings)
                .verticalScroll(rememberScrollState())
        ) {
            InfoText(
                text = stringResource(R.string.DuressPinSelectAccounts_Description),
                paddingBottom = 32.dp
            )

            if (regularAccounts.isNotEmpty()) {
                ItemsSection(
                    title = stringResource(R.string.DuressPinSelectAccounts_SectionWallets_Title),
                    items = regularAccounts,
                    selected = selected
                ) { account, checked ->
                    if (checked) {
                        selected.add(account.id)
                    } else {
                        selected.remove(account.id)
                    }
                }
                VSpacer(height = 32.dp)
            }

            if (watchAccounts.isNotEmpty()) {
                ItemsSection(
                    title = stringResource(R.string.DuressPinSelectAccounts_SectionWatchWallets_Title),
                    items = watchAccounts,
                    selected = selected
                ) { account, checked ->
                    if (checked) {
                        selected.add(account.id)
                    } else {
                        selected.remove(account.id)
                    }
                }
                VSpacer(height = 32.dp)
            }
        }
    }

}

@Composable
private fun ItemsSection(
    title: String,
    items: List<Account>,
    selected: SnapshotStateList<String>,
    onToggle: (Account, Boolean) -> Unit,
) {
    HeaderText(text = title)
    CellUniversalLawrenceSection(items) { account ->
        val checked = selected.contains(account.id)
        RowUniversal(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clickable {
                    onToggle.invoke(account, !checked)
                }
        ) {
            Column {
                body_leah(text = account.name)
                VSpacer(height = 1.dp)
                if (!account.hasAnyBackup) {
                    subhead2_lucian(text = stringResource(id = R.string.ManageAccount_BackupRequired_Title))
                } else {
                    subhead2_grey(
                        text = account.type.detailedDescription,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }
            HFillSpacer(minWidth = 8.dp)
            HsCheckbox(
                checked = checked,
                onCheckedChange = {
                    onToggle.invoke(account, it)
                },
            )
        }
    }
}