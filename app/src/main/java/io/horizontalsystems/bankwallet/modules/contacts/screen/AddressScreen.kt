package io.horizontalsystems.bankwallet.modules.contacts.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Caution
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.bankwallet.modules.contacts.viewmodel.AddressViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_lucian
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressScreen(
    viewModel: AddressViewModel,
    onNavigateToBlockchainSelector: () -> Unit,
    onDone: (ContactAddress) -> Unit,
    onDelete: (ContactAddress) -> Unit,
    onNavigateToBack: () -> Unit
) {
    val uiState = viewModel.uiState

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    HSScaffold(
        title = uiState.headerTitle.getString(),
        onBack = onNavigateToBack,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Done),
                enabled = uiState.doneEnabled,
                tint = ComposeAppTheme.colors.jacob,
                onClick = {
                    uiState.addressState?.dataOrNull?.let {
                        onDone(ContactAddress(uiState.blockchain, it.hex))
                    }
                }
            )
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VSpacer(12.dp)

            CellUniversalLawrenceSection(
                listOf {
                    RowUniversal(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        onClick = if (uiState.canChangeBlockchain) onNavigateToBlockchainSelector else null
                    ) {
                        subhead2_grey(
                            text = stringResource(R.string.AddToken_Blockchain),
                            modifier = Modifier.weight(1f)
                        )
                        subhead1_leah(
                            text = uiState.blockchain.name,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        if (uiState.canChangeBlockchain) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_down_arrow_20),
                                contentDescription = null,
                                tint = ComposeAppTheme.colors.grey
                            )
                        }
                    }
                }
            )

            VSpacer(32.dp)

            FormsInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                initial = uiState.address,
                hint = stringResource(R.string.Contacts_AddressHint),
                state = uiState.addressState,
                qrScannerEnabled = true,
            ) {
                viewModel.onEnterAddress(it)
            }
            if (uiState.showDelete) {
                VSpacer(32.dp)
                DeleteAddressButton {
                    showBottomSheet = true
                }
            }
        }
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState,
                containerColor = ComposeAppTheme.colors.transparent
            ) {
                ConfirmationBottomSheet(
                    title = stringResource(R.string.Contacts_DeleteAddress),
                    text = stringResource(R.string.Contacts_DeleteAddress_Warning),
                    iconPainter = painterResource(R.drawable.ic_delete_20),
                    iconTint = ColorFilter.tint(ComposeAppTheme.colors.lucian),
                    confirmText = stringResource(R.string.Button_Delete),
                    cautionType = Caution.Type.Error,
                    cancelText = stringResource(R.string.Button_Cancel),
                    onConfirm = {
                        uiState.editingAddress?.let { onDelete(it) }
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    },
                    onClose = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DeleteAddressButton(onClick: () -> Unit) {
    CellUniversalLawrenceSection(
        listOf {
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
                    text = stringResource(R.string.Contacts_DeleteAddress),
                )
            }

        }
    )
}
