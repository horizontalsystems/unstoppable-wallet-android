package io.horizontalsystems.bankwallet.modules.send

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.address.AddressParserModule
import io.horizontalsystems.bankwallet.modules.address.AddressParserViewModel
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.availablebalance.AvailableBalance2
import io.horizontalsystems.bankwallet.modules.sendtokenselect.PrefilledData
import io.horizontalsystems.bankwallet.ui.AmountInput
import io.horizontalsystems.bankwallet.ui.AmountSuggestionBar
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import java.math.BigDecimal

@Composable
fun SendScreen(
    title: String,
    onCloseClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = title,
            navigationIcon = {
                HsBackButton(onClick = onCloseClick)
            },
            menuItems = listOf()
        )

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            content.invoke(this)
        }
    }
}

@Composable
fun SendScreenCommon(
    navController: NavController,
    wallet: Wallet,
    prefilledData: PrefilledData?,
    title: String,
    coinDecimals: Int,
    onEnterAmount: (BigDecimal?) -> Unit,
    onEnterAmountPercentage: (Int) -> Unit,
    onEnterFiatAmount: (BigDecimal?) -> Unit,
    onEnterAddress: (Address?) -> Unit,
    onProceed: () -> Unit,
    uiState: SendUiState,
) {
    val availableBalance = uiState.availableBalance
    val amountCaution = uiState.amountCaution
    val addressError = uiState.addressError
    val canBeSend = uiState.canBeSend
    val showAddressInput = uiState.showAddressInput
    val currency = uiState.currency
    val amount = uiState.amount
    val fiatAmountInputEnabled = uiState.fiatAmountInputEnabled
    val fiatAmount = uiState.fiatAmount

    val paymentAddressViewModel = viewModel<AddressParserViewModel>(
        factory = AddressParserModule.Factory(wallet.token, prefilledData?.amount)
    )
    val amountParsedFromAddress = paymentAddressViewModel.amountUnique
    LaunchedEffect(amountParsedFromAddress) {
        amountParsedFromAddress?.let {
            onEnterAmount.invoke(it.amount)
        }
    }

    ComposeAppTheme {
        var amountInputHasFocus by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            SendScreen(
                title = title,
                onCloseClick = { navController.popBackStack() }
            ) {
                val focusRequester = remember { FocusRequester() }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                Spacer(modifier = Modifier.height(12.dp))

                val borderColor = when (amountCaution?.type) {
                    HSCaution.Type.Error -> ComposeAppTheme.colors.red50
                    HSCaution.Type.Warning -> ComposeAppTheme.colors.yellow50
                    else -> ComposeAppTheme.colors.steel20
                }

                AmountInput(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                        .background(ComposeAppTheme.colors.lawrence)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .onFocusChanged {
                            amountInputHasFocus = it.hasFocus
                        },
                    coinAmount = amount,
                    onValueChange = onEnterAmount,
                    fiatAmount = fiatAmount,
                    currency = currency,
                    onFiatValueChange = onEnterFiatAmount,
                    fiatAmountInputEnabled = fiatAmountInputEnabled,
                    focusRequester = focusRequester,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AvailableBalance2(
                    coinCode = wallet.coin.code,
                    coinDecimal = coinDecimals,
                    availableBalance = availableBalance
                )

                if (showAddressInput) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HSAddressInput(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        initial = prefilledData?.address?.let { Address(it) },
                        tokenQuery = wallet.token.tokenQuery,
                        coinCode = wallet.coin.code,
                        error = addressError,
                        textPreprocessor = paymentAddressViewModel,
                        navController = navController,
                        onValueChange = onEnterAddress
                    )
                }
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    title = stringResource(R.string.Send_DialogProceed),
                    onClick = onProceed,
                    enabled = canBeSend
                )
            }

            AmountSuggestionBar(
                availableBalance = availableBalance,
                amount = amount,
                onEnterAmountPercentage = onEnterAmountPercentage,
                onDelete = {
                    onEnterAmount.invoke(null)
                },
                inputHasFocus = amountInputHasFocus
            )
        }
    }
}
