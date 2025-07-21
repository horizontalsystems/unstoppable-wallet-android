package cash.p.terminal.modules.send.evm

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.authorizedAction
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.address.AddressParserModule
import cash.p.terminal.modules.address.AddressParserViewModel
import cash.p.terminal.modules.address.HSAddressCell
import cash.p.terminal.modules.amount.AmountInputModeViewModel
import cash.p.terminal.modules.amount.HSAmountInput
import cash.p.terminal.modules.availablebalance.AvailableBalance
import cash.p.terminal.modules.pin.ConfirmPinFragment
import cash.p.terminal.modules.pin.PinType
import cash.p.terminal.modules.send.SendScreen
import cash.p.terminal.modules.send.evm.confirmation.SendEvmConfirmationFragment
import cash.p.terminal.modules.sendtokenselect.PrefilledData
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.helpers.HudHelper
import java.math.BigDecimal

@Composable
fun SendEvmScreen(
    title: String,
    navController: NavController,
    amountInputModeViewModel: AmountInputModeViewModel,
    address: Address,
    wallet: Wallet,
    amount: BigDecimal?,
    hideAddress: Boolean,
    sendEntryPointDestId: Int,
) {
    val viewModel =
        viewModel<SendEvmViewModel>(factory = SendEvmModule.Factory(wallet, address, hideAddress))
    val uiState = viewModel.uiState

    val availableBalance = uiState.availableBalance
    val amountCaution = uiState.amountCaution
    val proceedEnabled = uiState.canBeSend
    val amountInputType = amountInputModeViewModel.inputType

    val paymentAddressViewModel = viewModel<AddressParserViewModel>(
        factory = AddressParserModule.Factory(wallet.token,
            PrefilledData(uiState.address.hex, amount)
        )
    )
    val amountUnique = paymentAddressViewModel.amountUnique
    val view = LocalView.current

    cash.p.terminal.ui_compose.theme.ComposeAppTheme {
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        SendScreen(
            title = title,
            onCloseClick = { navController.popBackStack() }
        ) {
            if (uiState.showAddressInput) {
                HSAddressCell(
                    title = stringResource(R.string.Send_Confirmation_To),
                    value = uiState.address.hex
                ) {
                    navController.popBackStack()
                }
                VSpacer(16.dp)
            }

            HSAmountInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                focusRequester = focusRequester,
                availableBalance = availableBalance,
                caution = amountCaution,
                coinCode = wallet.coin.code,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                onClickHint = {
                    amountInputModeViewModel.onToggleInputType()
                },
                onValueChange = {
                    viewModel.onEnterAmount(it)
                },
                inputType = amountInputType,
                rate = viewModel.coinRate,
                amountUnique = amountUnique
            )

            Spacer(modifier = Modifier.height(12.dp))
            AvailableBalance(
                coinCode = wallet.coin.code,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                availableBalance = availableBalance,
                amountInputType = amountInputType,
                rate = viewModel.coinRate
            )


            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                title = stringResource(R.string.Send_DialogProceed),
                onClick = {
                    val sendData = viewModel.getSendData() ?: return@ButtonPrimaryYellow

                    if (viewModel.hasConnection()) {
                        navController.authorizedAction(
                            ConfirmPinFragment.InputConfirm(
                                descriptionResId = R.string.Unlock_EnterPasscode,
                                pinType = PinType.TRANSFER
                            )
                        ) {
                            navController.slideFromRight(
                                R.id.sendEvmConfirmationFragment,
                                SendEvmConfirmationFragment.Input(
                                    sendData = sendData,
                                    blockchainType = viewModel.wallet.token.blockchainType,
                                    sendEntryPointDestId = sendEntryPointDestId

                                )
                            )
                        }
                    } else {
                        HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
                    }
                },
                enabled = proceedEnabled
            )
        }
    }
}
