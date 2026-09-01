package io.horizontalsystems.walletkit.modules.crosspay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.badge
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.address.AddressParserModule
import io.horizontalsystems.walletkit.modules.address.AddressParserViewModel
import io.horizontalsystems.walletkit.modules.enteraddress.EnterAddressViewModel
import io.horizontalsystems.walletkit.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.walletkit.modules.multiswap.AmountInput
import io.horizontalsystems.walletkit.modules.multiswap.FiatAmountInput
import io.horizontalsystems.walletkit.modules.multiswap.SwapSelectCoinPage
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.BadgeText
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.walletkit.ui.compose.components.CoinImage
import io.horizontalsystems.walletkit.ui.compose.components.FormsInputAddress
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.headline1_leah
import io.horizontalsystems.walletkit.ui.compose.components.headline2_jacob
import io.horizontalsystems.walletkit.ui.compose.components.headline2_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_grey
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_lucian
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.marketkit.models.Token
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Cross-chain payment from [Input.wallet]'s token to an exact amount of any
 * provider-supported cryptocurrency at an external address, powered by the
 * exact-output swap rail.
 */
@Serializable
data class CrossPayPage(val input: Input) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        CrossPayScreen(navigation, input.wallet)
    }

    @Serializable
    data class Input(val wallet: Wallet)
}

@Composable
private fun CrossPayScreen(navigation: HSNavigation, wallet: Wallet) {
    val viewModel = viewModel<CrossPayViewModel>(factory = CrossPayViewModel.Factory(wallet))
    val uiState = viewModel.uiState

    val selectTokenTitle = stringResource(R.string.CrossPay_SendTo)
    val onClickTokenSelect = navigation.slideFromBottomForResult<Token>(
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

    HSScaffold(
        title = stringResource(R.string.Balance_Pay),
        onBack = navigation::removeLastOrNull,
    ) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.ime)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
            ) {
                BalanceHeader(uiState)

                subhead2_grey(
                    text = stringResource(R.string.CrossPay_SendTo),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                )
                TokenOutSelector(uiState.tokenOut, onClickTokenSelect)

                uiState.tokenOut?.let { tokenOut ->
                    RecipientAddressInput(tokenOut, viewModel::onEnterRecipient)

                    subhead2_grey(
                        text = stringResource(R.string.CrossPay_Amount),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    )
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        AmountInput(
                            value = uiState.amountOut,
                            onValueChange = viewModel::onEnterAmount,
                        )
                        VSpacer(height = 3.dp)
                        FiatAmountInput(
                            value = uiState.fiatAmountOut,
                            currency = uiState.currency,
                            onValueChange = viewModel::onEnterFiatAmount,
                            enabled = true,
                        )
                    }

                    QuoteRow(uiState)
                }
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.CrossPay_Review),
                    enabled = uiState.canReview,
                    onClick = {
                        val tokenOut = uiState.tokenOut
                        val recipient = uiState.recipient
                        val amountOut = uiState.amountOut
                        if (tokenOut != null && recipient != null && amountOut != null) {
                            navigation.slideFromRight(
                                CrossPayConfirmationPage(
                                    CrossPayConfirmationPage.Input(
                                        wallet = wallet,
                                        tokenOut = tokenOut,
                                        recipient = recipient.hex,
                                        amount = amountOut,
                                    )
                                )
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BalanceHeader(uiState: CrossPayUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        headline1_leah(
            text = CoinValue(uiState.tokenIn, uiState.availableBalance ?: BigDecimal.ZERO).getFormattedFull()
        )
        VSpacer(height = 4.dp)
        subhead2_grey(text = stringResource(R.string.Swap_AvailableBalance))
    }
}

@Composable
private fun TokenOutSelector(tokenOut: Token?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoinImage(
            token = tokenOut,
            modifier = Modifier.size(32.dp)
        )
        HSpacer(width = 16.dp)
        if (tokenOut != null) {
            Column {
                headline2_leah(text = tokenOut.coin.code)
                VSpacer(height = 5.dp)
                BadgeText(
                    text = tokenOut.badge ?: stringResource(id = R.string.CoinPlatforms_Native),
                    background = ComposeAppTheme.colors.blade,
                    textColor = ComposeAppTheme.colors.leah,
                )
            }
        } else {
            headline2_jacob(text = stringResource(R.string.CrossPay_SelectToken))
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.ic_arrow_down_20),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey,
        )
    }
}

/**
 * Inline recipient entry with the same validation stack the full-screen address flow uses.
 * Keyed by the destination token so switching it re-validates from scratch — an address
 * valid on the previous chain must not survive the switch.
 */
@Composable
private fun RecipientAddressInput(tokenOut: Token, onEnterRecipient: (io.horizontalsystems.walletkit.entities.Address?) -> Unit) {
    val key = tokenOut.tokenQuery.id
    val addressViewModel = viewModel<EnterAddressViewModel>(
        key = "crosspay_address_$key",
        factory = EnterAddressViewModel.Factory(
            token = tokenOut,
            address = null,
            allowNull = false,
        )
    )
    val parserViewModel = viewModel<AddressParserViewModel>(
        key = "crosspay_parser_$key",
        factory = AddressParserModule.Factory(token = tokenOut, prefilledAmount = null)
    )

    val addressUiState = addressViewModel.uiState

    LaunchedEffect(addressUiState.address, addressUiState.canBeSendToAddress) {
        onEnterRecipient(addressUiState.address.takeIf { addressUiState.canBeSendToAddress })
    }

    FormsInputAddress(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        value = addressUiState.value,
        hint = stringResource(id = R.string.Send_Hint_Address),
        state = addressUiState.inputState,
        showStateIcon = false,
        textPreprocessor = parserViewModel,
    ) {
        addressViewModel.onEnterAddress(it)
    }
}

@Composable
private fun QuoteRow(uiState: CrossPayUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoinImage(
            token = uiState.tokenIn,
            modifier = Modifier.size(32.dp)
        )
        HSpacer(width = 16.dp)
        headline2_leah(text = uiState.tokenIn.coin.code)
        Spacer(modifier = Modifier.weight(1f))
        when (val quote = uiState.quote) {
            null -> subhead2_grey(text = CoinValue(uiState.tokenIn, BigDecimal.ZERO).getFormattedFull())

            CrossPayQuoteState.Loading -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = ComposeAppTheme.colors.grey,
                strokeWidth = 2.dp
            )

            is CrossPayQuoteState.Success -> {
                val insufficient = uiState.availableBalance != null && quote.sellAmount > uiState.availableBalance
                val formatted = CoinValue(uiState.tokenIn, quote.sellAmount).getFormattedFull()
                if (insufficient) {
                    subhead2_lucian(text = formatted)
                } else {
                    headline2_leah(text = formatted)
                }
            }

            is CrossPayQuoteState.Error -> subhead2_lucian(text = quoteErrorText(quote, uiState))
        }
    }
}

@Composable
private fun quoteErrorText(error: CrossPayQuoteState.Error, uiState: CrossPayUiState): String {
    val limit = error.amount?.let { amount ->
        uiState.tokenOut?.let { CoinValue(it, amount).getFormattedFull() } ?: amount.toPlainString()
    }

    return when (error.kind) {
        CrossPayQuoteState.ErrorKind.NotSupported -> stringResource(R.string.CrossPay_TokenNotSupported)
        CrossPayQuoteState.ErrorKind.NoRoute -> stringResource(R.string.CrossPay_NoRoute)
        CrossPayQuoteState.ErrorKind.BelowMinimum -> stringResource(R.string.CrossPay_BelowMinimum, limit ?: "")
        CrossPayQuoteState.ErrorKind.AboveMaximum -> stringResource(R.string.CrossPay_AboveMaximum, limit ?: "")
        CrossPayQuoteState.ErrorKind.Network -> stringResource(R.string.CrossPay_NetworkError)
    }
}
