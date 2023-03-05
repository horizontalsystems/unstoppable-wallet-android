package cash.p.terminal.modules.contacts.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.contacts.model.ContactAddress
import cash.p.terminal.modules.contacts.viewmodel.AddressViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AddressScreen(
    viewModel: AddressViewModel,
    onNavigateToBlockchainSelector: () -> Unit,
    onDone: (ContactAddress) -> Unit,
    onNavigateToBack: () -> Unit
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
                    title = stringResource(R.string.Contacts_DeleteAddress),
                    text = stringResource(R.string.Contacts_DeleteAddress_Warning),
                    onDelete = {
                        //viewModel.onDelete()
                    },
                    onClose = {
                        coroutineScope.launch { modalBottomSheetState.hide() }
                    }
                )
            }
        ) {
            Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
                AppBar(
                    title = uiState.headerTitle,
                    navigationIcon = {
                        HsBackButton(onNavigateToBack)
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Done),
                            enabled = uiState.doneEnabled,
                            onClick = {
                                onDone(ContactAddress(uiState.blockchain, "${uiState.blockchain.name}-address"))
                            }
                        )
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

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
            }
        }
    }
}
