package io.horizontalsystems.bankwallet.modules.sendx

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.fee.HSFeeInput
import io.horizontalsystems.bankwallet.modules.sendevm.AvailableBalance
import io.horizontalsystems.bankwallet.modules.sendevm.HSAmountInput
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem

@Composable
fun SendScreen(navController: NavController, wallet: Wallet) {
    val viewModel = viewModel<SendViewModel>(factory = SendModule.Factory(wallet))
    val uiState = viewModel.uiState

    val availableBalance = uiState.availableBalance
    val minimumSendAmount = uiState.minimumSendAmount
    val maximumSendAmount = uiState.maximumSendAmount
    val addressError = uiState.addressError
    val fee = uiState.fee
    val proceedEnabled = uiState.canBeSend

    ComposeAppTheme {
        val fullCoin = wallet.platformCoin.fullCoin
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Send_Title, fullCoin.coin.code),
                navigationIcon = {
                    CoinImage(
                        iconUrl = fullCoin.coin.iconUrl,
                        placeholder = fullCoin.iconPlaceholder,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .size(24.dp)
                    )
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                )
            )

            AvailableBalance(
                coin = wallet.coin,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                availableBalance = availableBalance,
                amountInputMode = viewModel.amountInputMode
            )

            Spacer(modifier = Modifier.height(12.dp))
            HSAmountInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                focusRequester = focusRequester,
                availableBalance = availableBalance,
                minimumSendAmount = minimumSendAmount,
                maximumSendAmount = maximumSendAmount,
                coin = wallet.coin,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fiatDecimal = viewModel.fiatMaxAllowedDecimals,
//                amountValidator = viewModel,
                onUpdateInputMode = {
                    viewModel.onUpdateAmountInputMode(it)
                },
                onValueChange = {
                    Log.e("AAA", "onEnterAmount: $it")
                    viewModel.onEnterAmount(it)
                }
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
            HSFeeInput(
                coin = wallet.coin,
                coinDecimal = viewModel.coinMaxAllowedDecimals,
                fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                fee = fee,
                amountInputMode = viewModel.amountInputMode
            ) {

            }

            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                title = stringResource(R.string.Send_DialogProceed),
                onClick = {
//                    viewModel.sendData?.let {
//                        navController.slideFromRight(
//                            R.id.sendEvmFragment_to_sendEvmConfirmationFragment,
//                            SendEvmConfirmationModule.prepareParams(it)
//                        )
//                    }
                },
                enabled = proceedEnabled
            )
        }
    }

}

