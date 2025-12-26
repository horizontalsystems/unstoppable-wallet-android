package io.horizontalsystems.bankwallet.modules.contacts.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Caution
import io.horizontalsystems.bankwallet.modules.contacts.ContactsModule
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.ContactsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.ScreenMessageWithAction
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.bottom.BottomSearchBar
import io.horizontalsystems.bankwallet.uiv3.components.menu.MenuGroup
import io.horizontalsystems.bankwallet.uiv3.components.menu.MenuItemX
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ContactsScreenBottomSheetType {
    ReplaceAddressConfirmation, RestoreContactsConfirmation
}

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class
)
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

    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    try {
                        inputStream.bufferedReader().use { br ->
                            viewModel.restore(br.readText())

                            HudHelper.showSuccessMessage(
                                view,
                                R.string.Hud_Text_Done,
                                SnackbarDuration.SHORT
                            )
                        }
                    } catch (e: Throwable) {
                        HudHelper.showErrorMessage(view, e.message ?: e.javaClass.simpleName)
                    }
                }
            }
        }

    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    try {
                        outputStream.bufferedWriter().use { bw ->
                            bw.write(viewModel.backupJson)
                            bw.flush()

                            HudHelper.showSuccessMessage(
                                view,
                                R.string.Hud_Text_Done,
                                SnackbarDuration.SHORT
                            )
                        }
                    } catch (e: Throwable) {
                        HudHelper.showErrorMessage(view, e.message ?: e.javaClass.simpleName)
                    }
                }
            }
        }

    var bottomSheetType: ContactsScreenBottomSheetType? by remember { mutableStateOf(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var selectedContact by remember { mutableStateOf<Contact?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    HSScaffold(
        title = stringResource(R.string.Contacts),
        menuItems = buildList {
            if (uiState.showAddContact) {
                add(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Contacts_NewContact),
                        icon = R.drawable.icon_user_plus,
                        onClick = onNavigateToCreateContact
                    )
                )
            }
            if (uiState.showMoreOptions) {
                add(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Contacts_ActionMore),
                        icon = R.drawable.ic_more2_20,
                        enabled = true,
                        onClick = {
                            showMoreSelectorDialog = true
                        }
                    )
                )
            }
        },
        onBack = onNavigateToBack,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (uiState.contacts.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val lazyListState = rememberSaveable(
                        uiState.contacts.size,
                        saver = LazyListState.Saver
                    ) {
                        LazyListState()
                    }

                    LaunchedEffect(lazyListState.isScrollInProgress) {
                        if (lazyListState.isScrollInProgress) {
                            if (isSearchActive) {
                                isSearchActive = false
                            }
                        }
                    }
                    VSpacer(12.dp)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ComposeAppTheme.colors.lawrence),
                        state = lazyListState
                    ) {
                        itemsIndexed(uiState.contacts) { index, contact ->
                            Contact(contact) {
                                if (viewModel.shouldShowReplaceWarning(contact)) {
                                    coroutineScope.launch {
                                        bottomSheetType =
                                            ContactsScreenBottomSheetType.ReplaceAddressConfirmation
                                        selectedContact = contact
                                        coroutineScope.launch {
                                            bottomSheetState.show()
                                        }
                                    }
                                } else {
                                    isSearchActive = false
                                    coroutineScope.launch {
                                        delay(200)
                                        onNavigateToContact(contact)
                                    }
                                }
                            }
                            if (index < uiState.contacts.size - 1) {
                                HsDivider()
                            }
                        }
                    }
                    VSpacer(32.dp)
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
                MenuGroup(
                    title = stringResource(R.string.Contacts_ActionMore),
                    items = ContactsModule.ContactsAction.values().map {
                        (MenuItemX(stringResource(it.title), false, it))
                    },
                    onDismissRequest = {
                        showMoreSelectorDialog = false
                    },
                    onSelectItem = { action ->
                        when (action) {
                            ContactsModule.ContactsAction.Restore -> {
                                if (viewModel.shouldShowRestoreWarning()) {
                                    coroutineScope.launch {
                                        bottomSheetType =
                                            ContactsScreenBottomSheetType.RestoreContactsConfirmation
                                        coroutineScope.launch {
                                            bottomSheetState.show()
                                        }
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

            if (uiState.contacts.isNotEmpty()) {
                BottomSearchBar(
                    searchQuery = searchQuery,
                    isSearchActive = isSearchActive,
                    onActiveChange = { isSearchActive = it },
                    onSearchQueryChange = { query ->
                        searchQuery = query
                        viewModel.onEnterQuery(query)
                    },
                )
            }
        }
        bottomSheetType?.let { type ->
            ModalBottomSheet(
                onDismissRequest = {
                    coroutineScope.launch {
                        bottomSheetState.hide()
                    }
                    bottomSheetType = null
                },
                sheetState = bottomSheetState,
                containerColor = ComposeAppTheme.colors.transparent
            ) {
                when (type) {
                    ContactsScreenBottomSheetType.ReplaceAddressConfirmation -> {
                        val warningMessage = selectedContact?.let {
                            viewModel.replaceWarningMessage(it)?.getString()
                        }
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
                                coroutineScope.launch {
                                    bottomSheetState.hide()
                                    bottomSheetType = null
                                }
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
                                coroutineScope.launch {
                                    bottomSheetState.hide()
                                    bottomSheetType = null
                                }
                            }
                        )
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
            headline2_leah(
                text = contact.name,
                maxLines = 1
            )
            subhead2_grey(
                text = stringResource(
                    R.string.Contacts_AddressesCount,
                    contact.addresses.size
                )
            )
        }
        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
        )
    }
}
