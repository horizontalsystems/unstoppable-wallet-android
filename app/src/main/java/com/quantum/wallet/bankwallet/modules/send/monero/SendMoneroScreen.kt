package com.quantum.wallet.bankwallet.modules.send.monero

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.providers.Translator
import com.quantum.wallet.bankwallet.core.slideFromBottomForResult
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.modules.address.AddressParserModule
import com.quantum.wallet.bankwallet.modules.address.AddressParserViewModel
import com.quantum.wallet.bankwallet.modules.address.HSAddressCell
import com.quantum.wallet.bankwallet.modules.amount.AmountInputModeViewModel
import com.quantum.wallet.bankwallet.modules.amount.HSAmountInput
import com.quantum.wallet.bankwallet.modules.availablebalance.AvailableBalance
import com.quantum.wallet.bankwallet.modules.fee.HSFee
import com.quantum.wallet.bankwallet.modules.memo.HSMemoInput
import com.quantum.wallet.bankwallet.modules.send.AddressRiskyBottomSheetAlert
import com.quantum.wallet.bankwallet.modules.send.SendConfirmationFragment
import com.quantum.wallet.bankwallet.modules.send.SendScreen
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonPrimaryYellow
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import java.math.BigDecimal

@Composable
fun SendMoneroScreen(
    title: String,
    navController: NavController,
    viewModel: SendMoneroViewModel,
    amountInputModeViewModel: AmountInputModeViewModel,
    sendEntryPointDestId: Int,
    amount: BigDecimal?,
    memo: String?,
    riskyAddress: Boolean
) {
    val wallet = viewModel.wallet
    val uiState = viewModel.uiState

    val availableBalance = uiState.availableBalance
    val amountCaution = uiState.amountCaution
    val proceedEnabled = uiState.canBeSend
    val fee = uiState.fee
    val feeInProgress = uiState.feeInProgress
    val amountInputType = amountInputModeViewModel.inputType
    val keyboardController = LocalSoftwareKeyboardController.current

    val paymentAddressViewModel = viewModel<AddressParserViewModel>(
        factory = AddressParserModule.Factory(wallet.token, amount)
    )
    val amountUnique = paymentAddressViewModel.amountUnique


    ComposeAppTheme {
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        SendScreen(
            title = title,
            onBack = { navController.popBackStack() }
        ) {
            VSpacer(16.dp)
            if (uiState.showAddressInput) {
                HSAddressCell(
                    title = stringResource(R.string.Send_Confirmation_To),
                    value = uiState.address.hex,
                    riskyAddress = riskyAddress
                ) {
                    navController.popBackStack()
                }
                VSpacer(16.dp)
            }

            HSAmountInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                focusRequester = focusRequester,
                availableBalance = availableBalance ?: BigDecimal.ZERO,
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

            VSpacer(8.dp)
            AvailableBalance(
                coinCode = wallet.coin.code,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                availableBalance = availableBalance,
                amountInputType = amountInputType,
                rate = viewModel.coinRate
            )

            VSpacer(16.dp)
            HSMemoInput(maxLength = 120, memo) {
                viewModel.onEnterMemo(it)
            }

            VSpacer(16.dp)
            HSFee(
                coinCode = viewModel.feeToken.coin.code,
                coinDecimal = viewModel.feeTokenMaxAllowedDecimals,
                fee = fee,
                amountInputType = amountInputType,
                rate = viewModel.feeCoinRate,
                navController = navController,
            )

            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                title = stringResource(R.string.Button_Next),
                onClick = {
                    if (riskyAddress) {
                        keyboardController?.hide()
                        navController.slideFromBottomForResult<AddressRiskyBottomSheetAlert.Result>(
                            R.id.addressRiskyBottomSheetAlert,
                            AddressRiskyBottomSheetAlert.Input(
                                alertText = Translator.getString(R.string.Send_RiskyAddress_AlertText)
                            )
                        ) {
                            openConfirm(navController, sendEntryPointDestId)
                        }
                    } else {
                        openConfirm(navController, sendEntryPointDestId)
                    }
                },
                enabled = proceedEnabled
            )
        }
    }
}

private fun openConfirm(
    navController: NavController,
    sendEntryPointDestId: Int
) {
    navController.slideFromRight(
        R.id.sendConfirmation,
        SendConfirmationFragment.Input(
            SendConfirmationFragment.Type.Monero,
            sendEntryPointDestId
        )
    )
}
