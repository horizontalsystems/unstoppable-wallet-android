package cash.p.terminal.modules.contacts.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.modules.contacts.viewmodel.ContactsViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*

@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    onNavigateToBack: () -> Unit,
    onNavigateToCreateContact: () -> Unit,
    onNavigateToContact: (String) -> Unit
) {
    val uiState = viewModel.uiState

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Contacts),
                navigationIcon = { HsBackButton(onNavigateToBack) },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Contacts_Export),
                        icon = R.drawable.icon_search,
                        enabled = false,
                        onClick = { }
                    ),
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Contacts_Export),
                        icon = R.drawable.icon_export,
                        enabled = false,
                        onClick = { }
                    ),
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Contacts_NewContact),
                        icon = R.drawable.icon_user_plus,
                        tint = ComposeAppTheme.colors.jacob,
                        onClick = onNavigateToCreateContact
                    )
                )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(12.dp))
                CellUniversalLawrenceSection(uiState.contacts) { contact ->
                    Contact(contact) {
                        onNavigateToContact(contact.id)
                    }
                }
            }
        }
    }
}

@Composable
fun Contact(
    contact: Contact,
    onClick: () -> Unit
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.weight(1f)) {
            body_leah(
                text = contact.name,
                maxLines = 1
            )
            subhead2_grey(text = stringResource(R.string.Contacts_AddressesCount, contact.addresses.size))
        }
        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
        )
    }
}
