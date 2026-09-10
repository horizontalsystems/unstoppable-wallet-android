package io.horizontalsystems.walletkit.modules.crosspay

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.badge
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.walletkit.modules.multiswap.AmountInput
import io.horizontalsystems.walletkit.modules.multiswap.FiatAmountInput
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.send.AddressRiskySheet
import io.horizontalsystems.walletkit.modules.send.v2.AddressRow
import io.horizontalsystems.walletkit.modules.send.v2.SectionArrow
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.Keyboard
import io.horizontalsystems.walletkit.ui.compose.components.BadgeText
import io.horizontalsystems.walletkit.ui.compose.components.BoxTyler44
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.walletkit.ui.compose.components.ButtonSecondary
import io.horizontalsystems.walletkit.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.walletkit.ui.compose.components.CoinImage
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.caption_grey
import io.horizontalsystems.walletkit.ui.compose.components.headline2_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead1_grey
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_grey
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

/**
 * Body of the CrossPay tab on the unified send screen: destination token + exact amount the
 * recipient gets, their address, and a live "You will Pay" quote in the wallet's token.
 */
@Composable
fun CrossPayTabBody(
    navigation: HSNavigation,
    viewModel: CrossPayTabViewModel,
    keyboardState: Keyboard,
) {
    val uiState = viewModel.uiState
    val keyboardController = LocalSoftwareKeyboardController.current
    var amountInputHasFocus by remember { mutableStateOf(false) }
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

    val openCoinSelect = navigation.slideFromRightForResult<Token>(
        { CrossPayCoinPage(viewModel.tokenIn) }
    ) {
        viewModel.onSelectTokenOut(it)
    }

    val openAddress = navigation.slideFromRightForResult<CrossPayAddressPage.Result>(
        { CrossPayAddressPage(uiState.tokenOut!!, uiState.address?.hex) }
    ) {
        viewModel.onSelectAddress(it.address, it.risky)
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
            AmountOutSection(
                tokenIn = uiState.tokenIn,
                tokenOut = uiState.tokenOut,
                amount = uiState.amountOut,
                fiatAmount = uiState.fiatAmountOut,
                fiatAmountInputEnabled = uiState.fiatAmountInputEnabled,
                currency = uiState.currency,
                availableBalance = uiState.availableBalance,
                insufficient = uiState.step is CrossPayStep.InsufficientBalance,
                focusRequester = focusRequester,
                onValueChange = viewModel::onEnterAmount,
                onFiatValueChange = viewModel::onEnterFiatAmount,
                onFocusChanged = { amountInputHasFocus = it },
                onClickToken = openCoinSelect,
            )
            SectionArrow()
            AddressRow(
                address = uiState.address,
                onClick = { if (uiState.tokenOut != null) openAddress() },
            )
            HsDivider(modifier = Modifier.fillMaxWidth())
            YouWillPayRow(uiState)
            VSpacer(32.dp)
        }

        CrossPayInfoCard(uiState.tokenIn)
        VSpacer(16.dp)

        val buttonTitle = when (val step = uiState.step) {
            CrossPayStep.EnterAmount -> stringResource(R.string.Send_EnterAmount)
            CrossPayStep.EnterAddress -> stringResource(R.string.Send_EnterAddress)
            CrossPayStep.InsufficientBalance -> stringResource(R.string.CrossPay_InsufficientBalance)
            is CrossPayStep.QuoteError -> quoteErrorText(step.error, uiState.tokenOut)
            CrossPayStep.Quoting,
            CrossPayStep.Proceed -> stringResource(R.string.Button_Next)
        }
        ButtonPrimaryYellow(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            title = buttonTitle,
            enabled = uiState.step is CrossPayStep.Proceed,
            onClick = {
                if (uiState.riskyAddress) {
                    keyboardController?.hide()
                    confirmRiskyAddress()
                } else {
                    proceed()
                }
            },
        )
        if (amountInputHasFocus && keyboardState == Keyboard.Opened) {
            VSpacer(16.dp)
            QuickAmountsBar(
                onSelect = viewModel::onSelectQuickAmount,
                onDelete = { viewModel.onEnterAmount(null) },
                deleteEnabled = uiState.amountOut != null,
            )
        } else {
            VSpacer(16.dp)
        }
    }
}

@Composable
private fun AmountOutSection(
    tokenIn: Token,
    tokenOut: Token?,
    amount: BigDecimal?,
    fiatAmount: BigDecimal?,
    fiatAmountInputEnabled: Boolean,
    currency: io.horizontalsystems.walletkit.entities.Currency,
    availableBalance: BigDecimal?,
    insufficient: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (BigDecimal?) -> Unit,
    onFiatValueChange: (BigDecimal?) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onClickToken: () -> Unit,
) {
    Column(
        modifier = Modifier
            .onFocusChanged { onFocusChanged(it.hasFocus) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // The balance shown is the SOURCE token's — what funds the payment.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            availableBalance?.let {
                Text(
                    text = stringResource(
                        R.string.Send_Available,
                        App.numberFormatter.formatCoinFull(it, tokenIn.coin.code, tokenIn.decimals)
                    ),
                    style = ComposeAppTheme.typography.caption,
                    color = ComposeAppTheme.colors.ocean,
                )
            }
        }
        VSpacer(8.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClickToken,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoinImage(
                    token = tokenOut,
                    modifier = Modifier.size(40.dp)
                )
                HSpacer(16.dp)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        headline2_leah(
                            text = tokenOut?.coin?.code
                                ?: stringResource(R.string.CrossPay_ChooseCoin)
                        )
                        HSpacer(4.dp)
                        Icon(
                            painter = painterResource(R.drawable.arrow_s_down_20),
                            contentDescription = null,
                            tint = ComposeAppTheme.colors.leah,
                        )
                    }
                    tokenOut?.let {
                        VSpacer(5.dp)
                        BadgeText(
                            text = it.badge ?: stringResource(R.string.CoinPlatforms_Native),
                            background = ComposeAppTheme.colors.blade,
                            textColor = ComposeAppTheme.colors.leah,
                        )
                    }
                }
            }
            HSpacer(8.dp)
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                AmountInput(
                    value = amount,
                    onValueChange = onValueChange,
                    focusRequester = focusRequester,
                    textColor = if (insufficient) {
                        ComposeAppTheme.colors.lucian
                    } else {
                        ComposeAppTheme.colors.leah
                    },
                )
                if (fiatAmountInputEnabled || fiatAmount != null) {
                    VSpacer(3.dp)
                    FiatAmountInput(
                        value = fiatAmount,
                        currency = currency,
                        onValueChange = onFiatValueChange,
                        enabled = fiatAmountInputEnabled,
                    )
                }
            }
        }
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
private fun CrossPayInfoCard(tokenIn: Token) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .border(1.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_info_filled_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey,
                modifier = Modifier.size(20.dp),
            )
            HSpacer(8.dp)
            subhead1_grey(text = stringResource(R.string.CrossPay_Info_Title))
        }
        VSpacer(8.dp)
        caption_grey(
            text = stringResource(R.string.CrossPay_Info_Description, tokenIn.coin.code),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QuickAmountsBar(
    onSelect: (Int) -> Unit,
    onDelete: () -> Unit,
    deleteEnabled: Boolean,
) {
    BoxTyler44(borderTop = true) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            CrossPayTabViewModel.QUICK_AMOUNTS.forEach { amount ->
                ButtonSecondary(onClick = { onSelect(amount) }) {
                    Text(
                        text = amount.toString(),
                        style = ComposeAppTheme.typography.captionSB,
                        color = ComposeAppTheme.colors.leah,
                    )
                }
            }
            ButtonSecondaryCircle(
                icon = R.drawable.ic_delete_20,
                enabled = deleteEnabled,
                tint = if (deleteEnabled) {
                    ComposeAppTheme.colors.leah
                } else {
                    ComposeAppTheme.colors.andy
                },
                onClick = onDelete,
            )
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
