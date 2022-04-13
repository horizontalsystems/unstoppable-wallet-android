package io.horizontalsystems.bankwallet.modules.sendx

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.amount.HSAmountInput
import io.horizontalsystems.bankwallet.modules.fee.FeeRateCaution
import io.horizontalsystems.bankwallet.modules.fee.HSFeeInputRaw
import io.horizontalsystems.bankwallet.modules.hodler.HSHodlerInput
import io.horizontalsystems.bankwallet.modules.sendevm.AvailableBalance
import io.horizontalsystems.bankwallet.modules.xrate.XRateViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.HSSectionRounded

@Composable
fun SendBitcoinScreen(
    navController: NavController,
    viewModel: SendBitcoinViewModel,
    xRateViewModel: XRateViewModel,
    amountInputModeViewModel: AmountInputModeViewModel
) {
    val wallet = viewModel.wallet
    val uiState = viewModel.uiState
    val isLockTimeEnabled = viewModel.isLockTimeEnabled
    val lockTimeIntervals = viewModel.lockTimeIntervals

    val availableBalance = uiState.availableBalance
    val addressError = uiState.addressError
    val amountCaution = uiState.amountCaution
    val fee = uiState.fee
    val proceedEnabled = uiState.canBeSend
    val amountInputType = amountInputModeViewModel.inputType
    val feeRateCaution = uiState.feeRateCaution
    val lockTimeInterval = uiState.lockTimeInterval

    val rate = xRateViewModel.rate

    ComposeAppTheme {
        val fullCoin = wallet.platformCoin.fullCoin
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        SendScreen(
            navController = navController,
            fullCoin = fullCoin
        ) {
            AvailableBalance(
                coinCode = wallet.coin.code,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                availableBalance = availableBalance,
                amountInputType = amountInputType,
                rate = rate
            )

            Spacer(modifier = Modifier.height(12.dp))
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
                rate = rate
            )

            Spacer(modifier = Modifier.height(12.dp))
            HSAddressInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                coinType = wallet.coinType,
                coinCode = wallet.coin.code,
                error = addressError
            ) {
                viewModel.onEnterAddress(it)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HSSectionRounded {
                CellSingleLineLawrence {
                    HSFeeInputRaw(
                        coinCode = wallet.coin.code,
                        coinDecimal = viewModel.coinMaxAllowedDecimals,
                        fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                        fee = fee,
                        amountInputType = amountInputType,
                        rate = rate,
                        enabled = viewModel.feeRateChangeable,
                        onClick = {
                            navController.slideFromBottom(R.id.feeSettings)
                        }
                    )
                }
                if (isLockTimeEnabled) {
                    CellSingleLineLawrence(borderTop = true) {
                        HSHodlerInput(
                            lockTimeIntervals = lockTimeIntervals,
                            lockTimeInterval = lockTimeInterval,
                            onSelect = {
                                viewModel.onEnterLockTimeInterval(it)
                            }
                        )
                    }
                }
            }

            feeRateCaution?.let {
                FeeRateCaution(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    feeRateCaution = feeRateCaution
                )
            }

            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                title = stringResource(R.string.Send_DialogProceed),
                onClick = {
                    navController.slideFromRight(R.id.sendBitcoinConfirmation)
                },
                enabled = proceedEnabled
            )
        }
    }
}
