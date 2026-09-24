package io.horizontalsystems.walletkit.modules.crosspay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.walletkit.modules.multiswap.SwapSelectCoinPage
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.send.AddressRiskySheet
import io.horizontalsystems.walletkit.modules.send.v2.AddressRow
import io.horizontalsystems.walletkit.modules.send.v2.InfoCard
import io.horizontalsystems.walletkit.modules.send.v2.SectionArrow
import io.horizontalsystems.walletkit.modules.send.v2.SendAddressPage
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.subhead1_grey
import io.horizontalsystems.walletkit.uiv3.components.bottombars.ButtonsGroupVertical
import io.horizontalsystems.walletkit.uiv3.components.controls.AvailableBalanceRow
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import io.horizontalsystems.walletkit.uiv3.components.controls.TokenAmountInput
import java.math.BigDecimal

/**
 * Body of the CrossPay tab on the unified send screen: destination token + exact amount the
 * recipient gets, their address, and a live "You will Pay" quote in the wallet's token.
 */
@Composable
fun CrossPayTabBody(
    navigation: HSNavigation,
    viewModel: CrossPayTabViewModel,
) {
    val uiState = viewModel.uiState
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val proceed = {
        val tokenOut = uiState.tokenOut
        val address = uiState.address
        val amountOut = uiState.amountOut
        if (tokenOut != null && address != null && amountOut != null) {
            navigation.slideFromRight(
                CrossPayConfirmationPage(
                    CrossPayConfirmationPage.Input(
                        wallet = viewModel.wallet,
                        tokenOut = tokenOut,
                        recipient = address.hex,
                        amount = amountOut,
                    )
                )
            )
        }
    }
    val confirmRiskyAddress = navigation.slideFromBottomForResult<AddressRiskySheet.Result>(
        {
            AddressRiskySheet(
                AddressRiskySheet.Input(
                    alertText = Translator.getString(R.string.Send_RiskyAddress_AlertText)
                )
            )
        }
    ) {
        proceed()
    }

    // The same picker the swap screen opens for its "You Get" side, so both flows offer an
    // identical token universe. A pick the provider cannot route surfaces on the quote row
    // as "Token not supported" instead of being filtered out up front.
    val selectTokenTitle = stringResource(R.string.CrossPay_ChooseCoin)
    val openCoinSelect = navigation.slideFromBottomForResult<Token>(
        {
            SwapSelectCoinPage(
                SwapSelectCoinPage.Input(
                    viewModel.tokenIn,
                    selectTokenTitle,
                    allowExternalReceive = true,
                )
            )
        }
    ) {
        viewModel.onSelectTokenOut(it)
    }

    val openAddress = navigation.slideFromBottomForResult<SendAddressPage.Result>(
        { SendAddressPage(uiState.tokenOut!!, uiState.address?.hex) }
    ) {
        viewModel.onSelectAddress(it.address, it.risky, it.contactName)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // The balance shown is the SOURCE token's — what funds the payment.
            AvailableBalanceRow(
                balanceToken = uiState.tokenIn,
                availableBalance = uiState.availableBalance
            )

            Box {
                Column {
                    TokenAmountInput(
                        token = uiState.tokenOut,
                        amount = uiState.amountOut,
                        fiatAmount = uiState.fiatAmountOut,
                        fiatAmountInputEnabled = uiState.fiatAmountInputEnabled,
                        currency = uiState.currency,
                        focusRequester = focusRequester,
                        onValueChange = viewModel::onEnterAmount,
                        onFiatValueChange = viewModel::onEnterFiatAmount,
                        amountExceedsBalance = uiState.step is CrossPayStep.InsufficientBalance,
                        onTokenClick = openCoinSelect
                    )
                    AddressRow(
                        address = uiState.address,
                        onClick = { if (uiState.tokenOut != null) openAddress() },
                        contactName = uiState.contactName,
                        risky = uiState.riskyAddress
                    )
                }
                SectionArrow()
            }

            Box {
                YouWillPayRow(uiState)
                Column {
                    VSpacer(64.dp)
                    InfoCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = stringResource(R.string.CrossPay_Info_Title),
                        text = stringResource(R.string.CrossPay_Info_Description, uiState.tokenIn.coin.code)
                    )
                    VSpacer(32.dp)
                }
            }
        }

        val buttonTitle = when (val step = uiState.step) {
            CrossPayStep.EnterAmount -> stringResource(R.string.Send_EnterAmount)
            CrossPayStep.EnterAddress -> stringResource(R.string.Send_EnterAddress)
            CrossPayStep.InsufficientBalance -> stringResource(R.string.CrossPay_InsufficientBalance)
            is CrossPayStep.QuoteError -> quoteErrorText(step.error, uiState.tokenOut)
            CrossPayStep.Quoting,
            CrossPayStep.Proceed -> stringResource(R.string.Button_Next)
        }
        ButtonsGroupVertical {
            HSButton(
                modifier = Modifier.fillMaxWidth(),
                title = buttonTitle,
                enabled = uiState.step is CrossPayStep.Proceed,
            ) {
                if (uiState.riskyAddress) {
                    keyboardController?.hide()
                    confirmRiskyAddress()
                } else {
                    proceed()
                }
            }
        }
        VSpacer(16.dp)
    }
}

@Composable
private fun YouWillPayRow(uiState: CrossPayTabUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        subhead1_grey(text = stringResource(R.string.CrossPay_YouWillPay))
        HSpacer(8.dp)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            when (val quote = uiState.quote) {
                CrossPayQuoteState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = ComposeAppTheme.colors.grey,
                    strokeWidth = 2.dp,
                )

                is CrossPayQuoteState.Success -> Text(
                    text = CoinValue(uiState.tokenIn, quote.sellAmount).getFormattedFull(),
                    style = ComposeAppTheme.typography.subheadSB,
                    color = ComposeAppTheme.colors.jacob,
                )

                is CrossPayQuoteState.Error -> Text(
                    text = quoteErrorText(quote, uiState.tokenOut),
                    style = ComposeAppTheme.typography.subheadR,
                    color = ComposeAppTheme.colors.lucian,
                )

                null -> Text(
                    text = CoinValue(uiState.tokenIn, BigDecimal.ZERO).getFormattedFull(),
                    style = ComposeAppTheme.typography.subheadSB,
                    color = ComposeAppTheme.colors.jacob,
                )
            }
        }
    }
}

@Composable
private fun quoteErrorText(error: CrossPayQuoteState.Error, tokenOut: Token?): String {
    val limit = error.amount?.let { amount ->
        tokenOut?.let { CoinValue(it, amount).getFormattedFull() } ?: amount.toPlainString()
    }

    return when (error.kind) {
        CrossPayQuoteState.ErrorKind.NotSupported -> stringResource(R.string.CrossPay_TokenNotSupported)
        CrossPayQuoteState.ErrorKind.NoRoute -> stringResource(R.string.CrossPay_NoRoute)
        CrossPayQuoteState.ErrorKind.BelowMinimum -> stringResource(R.string.CrossPay_BelowMinimum, limit ?: "")
        CrossPayQuoteState.ErrorKind.AboveMaximum -> stringResource(R.string.CrossPay_AboveMaximum, limit ?: "")
        CrossPayQuoteState.ErrorKind.Network -> stringResource(R.string.CrossPay_NetworkError)
    }
}
