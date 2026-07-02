package io.horizontalsystems.core.modules.transactions

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
import io.horizontalsystems.core.R
import io.horizontalsystems.core.modules.contacts.model.Contact
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import io.horizontalsystems.core.modules.nav3.LocalResultEventBus
import io.horizontalsystems.core.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.ui.compose.components.HFillSpacer
import io.horizontalsystems.core.ui.compose.components.HSpacer
import io.horizontalsystems.core.ui.compose.components.InfoErrorMessageDefault
import io.horizontalsystems.core.ui.compose.components.InfoText
import io.horizontalsystems.core.ui.compose.components.VSpacer
import io.horizontalsystems.core.ui.compose.components.body_leah
import io.horizontalsystems.core.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.core.uiv3.components.HSScaffold
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class SelectContactPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        SelectContactScreen(navigation, input)
    }

    @Serializable
    data class Input(val selected: Contact?, val blockchainType: BlockchainType?)

    @Parcelize
    data class Result(val contact: Contact?) : Parcelable

}

@Composable
fun SelectContactScreen(navigation: HSNavigation, input: SelectContactPage.Input?) {
    val resultEventBus = LocalResultEventBus.current
    val viewModel = viewModel<SelectContactViewModel>(
        initializer = SelectContactViewModel.init(
            input?.selected,
            input?.blockchainType
        )
    )
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.Contacts),
        onBack = navigation::removeLastOrNull,
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
                        resultEventBus.sendResult(SelectContactPage.Result(contact))
                        navigation.removeLastOrNull()
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
