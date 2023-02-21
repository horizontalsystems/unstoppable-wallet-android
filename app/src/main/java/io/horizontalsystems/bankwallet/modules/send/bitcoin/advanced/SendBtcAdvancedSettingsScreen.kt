package io.horizontalsystems.bankwallet.modules.send.bitcoin.advanced

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.bankwallet.modules.hodler.HSHodlerInput
import io.horizontalsystems.bankwallet.modules.send.bitcoin.TransactionInputsSortInfoPage
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SendBtcAdvancedSettingsScreen(
    navController: NavHostController,
    blockchainType: BlockchainType,
    lockTimeEnabled: Boolean,
    lockTimeIntervals: List<LockTimeInterval?>,
    lockTimeInterval: LockTimeInterval?,
    onEnterLockTimeInterval: (LockTimeInterval?) -> Unit,
) {
    val viewModel: SendBtcAdvancedSettingsViewModel =
        viewModel(factory = SendBtcAdvancedSettingsModule.Factory(blockchainType))

    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    var selectedLockTimeInterval by remember { mutableStateOf(lockTimeInterval) }
    ComposeAppTheme {
        ModalBottomSheetLayout(
            sheetState = modalBottomSheetState,
            sheetBackgroundColor = ComposeAppTheme.colors.transparent,
            sheetContent = {
                BottomSheetTransactionOrderSelector(
                    items = viewModel.uiState.transactionSortOptions,
                    onSelect = { mode ->
                        viewModel.setTransactionMode(mode)
                    },
                    onCloseClick = {
                        coroutineScope.launch {
                            modalBottomSheetState.animateTo(ModalBottomSheetValue.Hidden)
                        }
                    }
                )
            },
        ) {
            Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
                AppBar(
                    title = TranslatableString.ResString(R.string.Send_Advanced),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    },
                )
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                    TransactionDataSortSettings(
                        navController,
                        viewModel.uiState.transactionSortTitle,
                    ) {
                        coroutineScope.launch {
                            modalBottomSheetState.animateTo(ModalBottomSheetValue.Expanded)
                        }
                    }

                    if (lockTimeEnabled) {
                        Spacer(Modifier.height(32.dp))
                        CellUniversalLawrenceSection(
                            listOf {
                                HSHodlerInput(
                                    lockTimeIntervals = lockTimeIntervals,
                                    lockTimeInterval = selectedLockTimeInterval,
                                    onSelect = {
                                        selectedLockTimeInterval = it
                                        onEnterLockTimeInterval(it)
                                    }
                                )
                            }
                        )
                        InfoText(
                            text = stringResource(R.string.Send_Hodler_Description),
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun BottomSheetTransactionOrderSelector(
    items: List<SendBtcAdvancedSettingsModule.SortModeViewItem>,
    onSelect: (TransactionDataSortMode) -> Unit,
    onCloseClick: () -> Unit
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_arrow_up_right_12),
        title = stringResource(R.string.BtcBlockchainSettings_TransactionSettings),
        onCloseClick = onCloseClick,
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.grey)
    ) {
        Spacer(Modifier.height(12.dp))
        CellUniversalLawrenceSection(items, showFrame = true) { item ->
            RowUniversal(
                onClick = {
                    onSelect.invoke(item.mode)
                    onCloseClick.invoke()
                },
            ) {
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    body_leah(text = stringResource(item.mode.title))
                    subhead2_grey(text = stringResource(item.mode.description))
                }
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.selected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_checkmark_20),
                            tint = ComposeAppTheme.colors.jacob,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(44.dp))
    }
}

@Composable
private fun TransactionDataSortSettings(
    navController: NavController,
    valueTitle: String,
    onClick: () -> Unit
) {
    HeaderText(
        text = stringResource(R.string.BtcBlockchainSettings_TransactionSettings),
        onInfoClick = {
            navController.navigate(TransactionInputsSortInfoPage)
        })
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal() {
                body_leah(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f),
                    text = stringResource(R.string.BtcBlockchainSettings_InputsOutputs)
                )
                ButtonSecondaryWithIcon(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .height(28.dp),
                    title = valueTitle,
                    iconRight = painterResource(R.drawable.ic_down_arrow_20),
                    onClick = onClick
                )
            }
        }
    )
    InfoText(
        text = stringResource(R.string.BtcBlockchainSettings_TransactionInputsOutputsSettingsDescription),
    )
}
