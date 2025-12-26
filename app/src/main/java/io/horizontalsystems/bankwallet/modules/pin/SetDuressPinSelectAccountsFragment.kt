package io.horizontalsystems.bankwallet.modules.pin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_lucian
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class SetDuressPinSelectAccountsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SetDuressPinSelectAccountsScreen(navController)
    }
}

@Composable
fun SetDuressPinSelectAccountsScreen(navController: NavController) {
    val viewModel =
        viewModel<SetDuressPinSelectAccountsViewModel>(factory = SetDuressPinSelectAccountsViewModel.Factory())
    val regularAccounts = viewModel.regularAccounts
    val watchAccounts = viewModel.watchAccounts
    val selected = remember { mutableStateListOf<String>() }

    HSScaffold(
        title = stringResource(R.string.DuressPinSelectAccounts_Title),
        onBack = navController::popBackStack,
        bottomBar = {
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.Button_Next),
                    onClick = {
                        navController.slideFromRight(
                            R.id.setDuressPinFragment,
                            SetDuressPinFragment.Input(selected)
                        )
                    },
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
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
                headline2_leah(text = account.name)
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