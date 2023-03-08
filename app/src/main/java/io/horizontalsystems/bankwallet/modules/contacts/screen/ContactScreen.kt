package io.horizontalsystems.bankwallet.modules.contacts.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.ContactViewModel
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.ContactViewModel.AddressViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ContactScreen(
    viewModel: ContactViewModel,
    onNavigateToBack: () -> Unit,
    onNavigateToAddress: (ContactAddress?) -> Unit
) {
    val uiState = viewModel.uiState
    val view = LocalView.current

    ComposeAppTheme {
        val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
        val coroutineScope = rememberCoroutineScope()

        ModalBottomSheetLayout(
            sheetState = modalBottomSheetState,
            sheetBackgroundColor = ComposeAppTheme.colors.transparent,
            sheetContent = {
                DeletionWarningBottomSheet(
                    title = stringResource(R.string.Contacts_DeleteContact),
                    text = stringResource(R.string.Contacts_DeleteContact_Warning),
                    onDelete = {
                        viewModel.onDelete()
                    },
                    onClose = {
                        coroutineScope.launch { modalBottomSheetState.hide() }
                    }
                )
            },
        ) {
            Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
                AppBar(
                    title = uiState.headerTitle,
                    navigationIcon = {
                        //TODO screen lagging when keyboard is open
                        HsBackButton(onNavigateToBack)
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Save),
                            enabled = uiState.saveEnabled,
                            onClick = viewModel::onSave
                        )
                    )
                )

                Spacer(Modifier.height(12.dp))
                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    initial = uiState.contactName,
                    pasteEnabled = false,
                    hint = stringResource(R.string.Contacts_NameHint),
                    onValueChange = viewModel::onNameChange
                )

                Addresses(
                    addressViewItems = uiState.addressViewItems,
                    onClickAddress = onNavigateToAddress
                )

                ActionButtons(
                    onAddAddress = {
                        onNavigateToAddress(null)
                    },
                    showDelete = uiState.showDelete,
                    onDeleteContact = {
                        coroutineScope.launch {
                            modalBottomSheetState.animateTo(ModalBottomSheetValue.Expanded)
                        }
                    }
                )

                LaunchedEffect(uiState.closeWithSuccess) {
                    if (uiState.closeWithSuccess) {
                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done, SnackbarDuration.SHORT)

                        //TODO screen lagging when keyboard is open
                        onNavigateToBack()
                    }
                }
            }
        }
    }
}

@Composable
fun DeletionWarningBottomSheet(
    title: String,
    text: String,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_delete_20),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.lucian),
        title = title,
        onCloseClick = onClose
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        TextImportantWarning(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = text
        )

        Spacer(modifier = Modifier.height(32.dp))
        ButtonPrimaryRed(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            title = stringResource(R.string.Button_Delete),
            onClick = {
                onDelete()
                onClose()
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        ButtonPrimaryTransparent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            title = stringResource(R.string.Button_Cancel),
            onClick = onClose
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ActionButtons(
    onAddAddress: () -> Unit,
    showDelete: Boolean,
    onDeleteContact: () -> Unit
) {
    Spacer(Modifier.height(32.dp))
    CellUniversalLawrenceSection(
        buildList {
            add {
                AddAddressButton(onAddAddress)
            }
            if (showDelete) {
                add {
                    DeleteContactButton(onDeleteContact)
                }
            }
        }
    )
}

@Composable
private fun DeleteContactButton(onClick: () -> Unit) {
    RowUniversal(
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier.padding(horizontal = 16.dp),
            painter = painterResource(R.drawable.ic_delete_20),
            contentDescription = null,
            tint = ComposeAppTheme.colors.lucian
        )
        body_lucian(
            text = stringResource(R.string.Contacts_DeleteContact),
        )
    }
}

@Composable
private fun AddAddressButton(onClick: () -> Unit) {
    RowUniversal(
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier.padding(horizontal = 16.dp),
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = null,
            tint = ComposeAppTheme.colors.jacob
        )
        body_jacob(text = stringResource(R.string.Contacts_AddAddress))
    }
}

@Composable
private fun Addresses(
    addressViewItems: List<AddressViewItem>,
    onClickAddress: (ContactAddress) -> Unit
) {
    if (addressViewItems.isNotEmpty()) {
        Spacer(Modifier.height(32.dp))
        CellUniversalLawrenceSection(addressViewItems) { addressViewItem ->
            ContactAddress(addressViewItem) {
                onClickAddress(addressViewItem.contactAddress)
            }
        }
    }
}

@Composable
private fun ContactAddress(
    addressViewItem: AddressViewItem,
    onClickEdit: () -> Unit
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClickEdit
    ) {
        Image(
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp),
            painter = rememberAsyncImagePainter(
                model = addressViewItem.blockchain.type.imageUrl,
                error = painterResource(R.drawable.ic_platform_placeholder_32)
            ),
            contentDescription = null,
        )
        Column(modifier = Modifier.weight(1f)) {
            body_leah(text = addressViewItem.blockchain.name)
            subhead2_grey(text = addressViewItem.contactAddress.address)
        }
        Spacer(Modifier.width(9.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_edit_20),
            contentDescription = null,
            tint = if (addressViewItem.edited) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.grey
        )
    }
}
