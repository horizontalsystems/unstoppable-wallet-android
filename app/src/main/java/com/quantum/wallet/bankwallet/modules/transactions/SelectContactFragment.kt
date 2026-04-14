package com.quantum.wallet.bankwallet.modules.transactions

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.getInput
import com.quantum.wallet.bankwallet.core.setNavigationResultX
import com.quantum.wallet.bankwallet.modules.contacts.model.Contact
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.HFillSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.HSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.InfoErrorMessageDefault
import com.quantum.wallet.bankwallet.ui.compose.components.InfoText
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.ui.compose.components.body_leah
import com.quantum.wallet.bankwallet.ui.compose.components.cell.CellUniversal
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
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
    val viewModel = viewModel<SelectContactViewModel>(
        initializer = SelectContactViewModel.init(
            input?.selected,
            input?.blockchainType
        )
    )
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.Contacts),
        onBack = navController::popBackStack,
    ) {
        if (uiState.items.isEmpty()) {
            Column {
                InfoText(text = stringResource(id = R.string.Transactions_Filter_ChooseContact_Hint))
                InfoErrorMessageDefault(
                    painter = painterResource(id = R.drawable.ic_user_24),
                    text = stringResource(R.string.Transactions_Filter_ChooseContact_NoSuitableContact)
                )
            }
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
