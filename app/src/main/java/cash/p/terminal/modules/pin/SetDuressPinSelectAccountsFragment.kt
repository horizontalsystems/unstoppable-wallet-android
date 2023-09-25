package cash.p.terminal.modules.pin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HFillSpacer
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.HsCheckbox
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_leah
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_lucian
import io.horizontalsystems.core.findNavController

class SetDuressPinSelectAccountsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            SetDuressPinSelectAccountsScreen(findNavController())
        }
    }
}

@Composable
fun SetDuressPinSelectAccountsScreen(navController: NavController) {
    val viewModel = viewModel<SetDuressPinSelectAccountsViewModel>(factory = SetDuressPinSelectAccountsViewModel.Factory())
    val items = viewModel.items
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
        }
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            InfoText(text = stringResource(R.string.DuressPinSelectAccounts_Description))

            CellUniversalLawrenceSection(items) { account ->
                RowUniversal(
                    modifier = Modifier.padding(horizontal = 16.dp)
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
                        checked = selected.contains(account.id),
                        onCheckedChange = { checked ->
                            if (checked) {
                                selected.add(account.id)
                            } else {
                                selected.remove(account.id)
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.Button_Continue),
                    onClick = {
                        navController.slideFromRight(R.id.setDuressPinFragment, SetDuressPinFragment.params(selected))
                    },
                )
            }
        }
    }

}