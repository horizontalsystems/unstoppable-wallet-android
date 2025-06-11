package io.horizontalsystems.bankwallet.modules.receive

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottomForResult
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.activatetoken.ActivateTokenFragment
import io.horizontalsystems.bankwallet.modules.receive.ui.ReceiveAddressScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import kotlinx.coroutines.launch

@Composable
fun ReceiveStellarAssetScreen(navController: NavController, wallet: Wallet, receiveEntryPointDestId: Int) {
    val viewModel = viewModel<ReceiveStellarAssetViewModel>(factory = ReceiveStellarAssetViewModel.Factory(wallet))
    val uiState = viewModel.uiState

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()

    val runActivation = {
        navController.slideFromBottomForResult<ActivateTokenFragment.Result>(
            R.id.activateTokenFragment,
            wallet
        ) {
            scope.launch {
                viewModel.onActivationResult(it.activated)
                sheetState.hide()
            }
        }
    }

    LaunchedEffect(uiState.activationRequired) {
        if (uiState.activationRequired) {
            sheetState.show()
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            BottomSheetHeader(
                iconPainter = painterResource(R.drawable.ic_attention_24),
                iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                title = stringResource(R.string.ActivationRequired_DialogTitle),
                onCloseClick = {
                    scope.launch {
                        sheetState.hide()
                    }
                }
            ) {
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    text = stringResource(R.string.ActivationRequired_DialogDescription, uiState.coinCode, uiState.coinCode)
                )
                VSpacer(12.dp)
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.Button_Activate),
                    onClick = runActivation
                )
                VSpacer(12.dp)
                ButtonPrimaryTransparent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.Button_Later),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                        }
                    }
                )
                VSpacer(32.dp)
            }
        }
    ) {
        ReceiveAddressScreen(
            title = stringResource(R.string.Deposit_Title, wallet.coin.code),
            uiState = uiState,
            setAmount = viewModel::setAmount,
            onErrorClick = viewModel::onErrorClick,
            slot1 = {
                if (uiState.trustlineEstablished == false) {
                    HsDivider(modifier = Modifier.fillMaxWidth())
                    RowUniversal(modifier = Modifier.height(48.dp)) {
                        subhead2_grey(
                            modifier = Modifier
                                .padding(start = 16.dp),
                            text = stringResource(R.string.Balance_Receive_Trustline),
                        )

                        HSpacer(8.dp)
                        HsIconButton(
                            modifier = Modifier.size(20.dp),
                            onClick = {
                                scope.launch {
                                    sheetState.show()
                                }
                            }
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_info_20),
                                contentDescription = null
                            )
                        }
                        subhead1_jacob(
                            text = stringResource(R.string.Hud_Text_Activate),
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .weight(1f)
                                .clickable(onClick = runActivation)
                        )
                    }
                }
            },
            onBackPress = { navController.popBackStack() },
            closeModule = {
                if (receiveEntryPointDestId == 0) {
                    navController.popBackStack()
                } else {
                    navController.popBackStack(receiveEntryPointDestId, true)
                }
            }
        )
    }
}
