package io.horizontalsystems.bankwallet.modules.contacts.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.contacts.ContactsModule
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.ContactsViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch

enum class ContactsScreenBottomSheetType {
    ReplaceAddressConfirmation, RestoreContactsConfirmation
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
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

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { inputStream ->
                try {
                    inputStream.bufferedReader().use { br ->
                        viewModel.restore(br.readText())

                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done, SnackbarDuration.SHORT)
                    }
                } catch (e: Throwable) {
                    HudHelper.showErrorMessage(view, e.message ?: e.javaClass.simpleName)
                }
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                try {
                    outputStream.bufferedWriter().use { bw ->
                        bw.write(viewModel.backupJson)
                        bw.flush()

                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done, SnackbarDuration.SHORT)
                    }
                } catch (e: Throwable) {
                    HudHelper.showErrorMessage(view, e.message ?: e.javaClass.simpleName)
                }
            }
        }
    }

    var bottomSheetType: ContactsScreenBottomSheetType? by remember { mutableStateOf(null) }
    val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    var selectedContact by remember { mutableStateOf<Contact?>(null) }

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            when (bottomSheetType) {
                null -> {
                    Spacer(modifier = Modifier.height(1.dp))
                }

                ContactsScreenBottomSheetType.ReplaceAddressConfirmation -> {
                    val warningMessage = selectedContact?.let { viewModel.replaceWarningMessage(it)?.getString() }
                    ConfirmationBottomSheet(
                        title = stringResource(R.string.Alert_TitleWarning),
                        text = warningMessage ?: "",
                        iconPainter = painterResource(R.drawable.icon_warning_2_20),
                        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                        confirmText = stringResource(R.string.Contacts_AddAddress_Replace),
                        cautionType = Caution.Type.Warning,
                        cancelText = stringResource(R.string.Button_Cancel),
                        onConfirm = {
                            selectedContact?.let {
                                coroutineScope.launch {
                                    bottomSheetState.hide()
                                    onNavigateToContact(it)
                                }
                            }
                        },
                        onClose = {
                            coroutineScope.launch { bottomSheetState.hide() }
                        }
                    )
                }

                ContactsScreenBottomSheetType.RestoreContactsConfirmation -> {
                    ConfirmationBottomSheet(
                        title = stringResource(R.string.Alert_TitleWarning),
                        text = stringResource(R.string.Contacts_Restore_Warning),
                        iconPainter = painterResource(R.drawable.icon_warning_2_20),
                        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                        confirmText = stringResource(R.string.Contacts_AddAddress_Replace),
                        cautionType = Caution.Type.Error,
                        cancelText = stringResource(R.string.Button_Cancel),
                        onConfirm = {
                            coroutineScope.launch {
                                bottomSheetState.hide()
                                restoreLauncher.launch(arrayOf("application/json"))
                            }
                        },
                        onClose = {
                            coroutineScope.launch { bottomSheetState.hide() }
                        }
                    )
                }
            }

        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = ComposeAppTheme.colors.tyler)
        ) {
            SearchBar(
                title = stringResource(R.string.Contacts),
                searchHintText = stringResource(R.string.Market_Search_Hint),
                menuItems = buildList {
                    if (uiState.showAddContact) {
                        add(
                            MenuItem(
                                title = TranslatableString.ResString(R.string.Contacts_NewContact),
                                icon = R.drawable.icon_user_plus,
                                tint = ComposeAppTheme.colors.jacob,
                                onClick = onNavigateToCreateContact
                            )
                        )
                    }
                    if (uiState.showMoreOptions) {
                        add(
                            MenuItem(
                                title = TranslatableString.ResString(R.string.Contacts_ActionMore),
                                icon = R.drawable.ic_more2_20,
                                tint = ComposeAppTheme.colors.jacob,
                                enabled = true,
                                onClick = {
                                    showMoreSelectorDialog = true
                                }
                            )
                        )
                    }
                },
                onClose = onNavigateToBack,
                onSearchTextChanged = { text ->
                    viewModel.onEnterQuery(text)
                }
            )
            if (uiState.contacts.isNotEmpty()) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(12.dp))
                    CellUniversalLawrenceSection(uiState.contacts) { contact ->
                        Contact(contact) {
                            if (viewModel.shouldShowReplaceWarning(contact)) {
                                coroutineScope.launch {
                                    bottomSheetType = ContactsScreenBottomSheetType.ReplaceAddressConfirmation
                                    selectedContact = contact
                                    bottomSheetState.show()
                                }
                            } else {
                                onNavigateToContact(contact)
                            }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            } else {
                if (uiState.searchMode) {
                    ListEmptyView(
                        text = stringResource(R.string.EmptyResults),
                        icon = R.drawable.ic_not_found
                    )
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
            }

            if (showMoreSelectorDialog) {
                SelectorDialogCompose(
                    title = stringResource(R.string.Contacts_ActionMore),
                    items = ContactsModule.ContactsAction.values().map {
                        (SelectorItem(stringResource(it.title), false, it))
                    },
                    onDismissRequest = {
                        showMoreSelectorDialog = false
                    },
                    onSelectItem = { action ->
                        when (action) {
                            ContactsModule.ContactsAction.Restore -> {
                                if (viewModel.shouldShowRestoreWarning()) {
                                    coroutineScope.launch {
                                        bottomSheetType = ContactsScreenBottomSheetType.RestoreContactsConfirmation
                                        bottomSheetState.show()
                                    }
                                } else {
                                    restoreLauncher.launch(arrayOf("application/json"))
                                }
                            }

                            ContactsModule.ContactsAction.Backup -> {
                                App.pinComponent.keepUnlocked()
                                backupLauncher.launch(viewModel.backupFileName)
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
