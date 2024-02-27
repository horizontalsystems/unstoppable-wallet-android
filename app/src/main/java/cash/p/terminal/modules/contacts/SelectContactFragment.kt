package cash.p.terminal.modules.contacts

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.getInput
import cash.p.terminal.core.setNavigationResultX
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.HFillSpacer
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_leah
import cash.p.terminal.ui.compose.components.cell.CellUniversal
import cash.p.terminal.ui.compose.components.title3_leah
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize

class SelectContactFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        SelectContactScreen(navController, navController.getInput())
    }

    @Parcelize
    data class Input(val selected: Contact?, val blockchainType: BlockchainType?) : Parcelable

    @Parcelize
    data class Result(val contact: Contact?) : Parcelable

}

@Composable
fun SelectContactScreen(navController: NavController, input: SelectContactFragment.Input?) {
    val viewModel = viewModel<SelectContactViewModel>(initializer = SelectContactViewModel.init(input?.selected, input?.blockchainType))
    val uiState = viewModel.uiState

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = {
                    title3_leah(text = stringResource(id = R.string.Contacts))
                },
                navigationIcon = {
                    HsBackButton(onClick = navController::popBackStack)
                },
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            if (uiState.items.isEmpty()) {
                ListEmptyView(
                    text = stringResource(R.string.Contacts_NoContacts),
                    icon = R.drawable.ic_user_24
                )
            } else {
                LazyColumn {
                    item {
                        InfoText(text = stringResource(id = R.string.Transactions_Filter_ChooseContact_Hint))
                    }
                    items(uiState.items) { contact ->
                        CellContact(contact, uiState.selected) {
                            navController.setNavigationResultX(SelectContactFragment.Result(contact))
                            navController.popBackStack()
                        }
                    }
                    item {
                        VSpacer(height = 32.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CellContact(
    contact: Contact?,
    selected: Contact?,
    onClick: () -> Unit,
) {
    CellUniversal(
        onClick = onClick
    ) {
        Icon(
            painter = if (contact == null) {
                painterResource(id = R.drawable.icon_paper_contract_24)
            } else {
                painterResource(id = R.drawable.ic_user_24)
            },
            contentDescription = "",
            tint = ComposeAppTheme.colors.grey
        )
        HSpacer(width = 16.dp)
        body_leah(text = contact?.name ?: stringResource(id = R.string.SelectContacts_All))
        if (contact == selected) {
            HFillSpacer(minWidth = 8.dp)
            Icon(
                painter = painterResource(id = R.drawable.icon_check_1_24),
                contentDescription = "selected",
                tint = ComposeAppTheme.colors.jacob
            )
        }
    }
}
