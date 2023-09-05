package io.horizontalsystems.bankwallet.modules.send.solana

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.address.AddressParserModule
import io.horizontalsystems.bankwallet.modules.address.AddressParserViewModel
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.amount.HSAmountInput
import io.horizontalsystems.bankwallet.modules.availablebalance.AvailableBalance
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationFragment
import io.horizontalsystems.bankwallet.modules.send.SendScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow

@Composable
fun SendSolanaScreen(
    navController: NavController,
    viewModel: SendSolanaViewModel,
    amountInputModeViewModel: AmountInputModeViewModel,
    sendEntryPointDestId: Int
) {
    val wallet = viewModel.wallet
    val uiState = viewModel.uiState

    val availableBalance = uiState.availableBalance
    val addressError = uiState.addressError
    val amountCaution = uiState.amountCaution
    val proceedEnabled = uiState.canBeSend
    val amountInputType = amountInputModeViewModel.inputType

    val paymentAddressViewModel = viewModel<AddressParserViewModel>(factory = AddressParserModule.Factory(wallet.token.blockchainType))
    val amountUnique = paymentAddressViewModel.amountUnique


    ComposeAppTheme {
        val fullCoin = wallet.token.fullCoin
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        SendScreen(
            fullCoin = fullCoin,
            onCloseClick = { navController.popBackStack() }
        ) {
            AvailableBalance(
                    coinCode = wallet.coin.code,
                    coinDecimal = viewModel.coinMaxAllowedDecimals,
                    fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                    availableBalance = availableBalance,
                    amountInputType = amountInputType,
                    rate = viewModel.coinRate
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
                    rate = viewModel.coinRate,
                    amountUnique = amountUnique
            )

            Spacer(modifier = Modifier.height(12.dp))
            HSAddressInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                tokenQuery = wallet.token.tokenQuery,
                coinCode = wallet.coin.code,
                error = addressError,
                textPreprocessor = paymentAddressViewModel,
                navController = navController
            ) {
                viewModel.onEnterAddress(it)
            }

            ButtonPrimaryYellow(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                    title = stringResource(R.string.Send_DialogProceed),
                    onClick = {
                        navController.slideFromRight(
                                R.id.sendConfirmation,
                                SendConfirmationFragment.prepareParams(
                                    SendConfirmationFragment.Type.Solana,
                                    sendEntryPointDestId
                                )
                        )
                    },
                    enabled = proceedEnabled
            )
        }
    }

}
