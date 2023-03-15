package cash.p.terminal.modules.contacts.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.contacts.ContactsModule
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.modules.contacts.viewmodel.ContactsViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    onNavigateToBack: () -> Unit,
    onNavigateToCreateContact: () -> Unit,
    onNavigateToContact: (Contact) -> Unit
) {
    val uiState = viewModel.uiState
    var showMoreSelectorDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { inputStream ->
                try {
                    inputStream.bufferedReader().use { br ->
                        viewModel.importContacts(br.readText())

                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done, SnackbarDuration.SHORT)
                    }
                } catch (e: Throwable) {
                    HudHelper.showErrorMessage(view, e.message ?: e.javaClass.simpleName)
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                try {
                    outputStream.bufferedWriter().use { bw ->
                        bw.write(viewModel.exportJsonData)
                        bw.flush()

                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done, SnackbarDuration.SHORT)
                    }
                } catch (e: Throwable) {
                    HudHelper.showErrorMessage(view, e.message ?: e.javaClass.simpleName)
                }
            }
        }
    }

    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Contacts),
                navigationIcon = { HsBackButton(onNavigateToBack) },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Search),
                        icon = R.drawable.icon_search,
                        enabled = false,
                        onClick = { }
                    ),
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Contacts_NewContact),
                        icon = R.drawable.icon_user_plus,
                        tint = ComposeAppTheme.colors.jacob,
                        onClick = onNavigateToCreateContact
                    ),
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Contacts_ActionMore),
                        icon = R.drawable.ic_more2_20,
                        enabled = true,
                        onClick = {
                            showMoreSelectorDialog = true
                        }
                    )
                )
            )
            if (uiState.contacts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))
                    CellUniversalLawrenceSection(uiState.contacts) { contact ->
                        Contact(contact) {
                            onNavigateToContact(contact)
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            } else {
                ScreenMessageWithAction(
                    text = stringResource(R.string.Contacts_NoContacts),
                    icon = R.drawable.icon_user_plus
                ) {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .padding(horizontal = 48.dp)
                            .fillMaxWidth(),
                        title = stringResource(R.string.Contacts_AddNewContact),
                        onClick = onNavigateToCreateContact
                    )
                }
            }

            if (showMoreSelectorDialog) {
                SelectorDialogCompose(
                    title = stringResource(R.string.Contacts_ActionMore),
                    items = ContactsModule.ContactsAction.values().map { (TabItem(stringResource(it.title), false, it)) },
                    onDismissRequest = {
                        showMoreSelectorDialog = false
                    },
                    onSelectItem = { action ->
                        when (action) {
                            ContactsModule.ContactsAction.Import -> {
                                importLauncher.launch(arrayOf("application/json"))
                            }
                            ContactsModule.ContactsAction.Export -> {
                                exportLauncher.launch(viewModel.exportFileName)
                            }
                        }
                    })
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
