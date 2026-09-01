package io.horizontalsystems.walletkit.modules.crosspay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.walletkit.entities.CurrencyValue
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.walletkit.modules.confirm.ErrorSheet
import io.horizontalsystems.walletkit.modules.evmfee.Cautions
import io.horizontalsystems.walletkit.modules.multiswap.FeeRow
import io.horizontalsystems.walletkit.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.walletkit.modules.multiswap.SwapInfoSheet
import io.horizontalsystems.walletkit.modules.multiswap.formatDuration
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldFeeTemplate
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.ConfirmationTopSection
import io.horizontalsystems.walletkit.serializers.BigDecimalSerializer
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.rememberAsyncAction
import io.horizontalsystems.walletkit.ui.helpers.TextHelper
import io.horizontalsystems.walletkit.uiv3.components.AlertCard
import io.horizontalsystems.walletkit.uiv3.components.AlertFormat
import io.horizontalsystems.walletkit.uiv3.components.AlertType
import io.horizontalsystems.walletkit.uiv3.components.cards.CardsErrorMessageDefault
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonConfig
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonSize
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class CrossPayConfirmationPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = viewModel<CrossPayConfirmViewModel>(
            initializer = CrossPayConfirmViewModel.init(
                CrossPayRequest(
                    tokenIn = input.wallet.token,
                    tokenOut = input.tokenOut,
                    recipient = input.recipient,
                    amountOut = input.amount,
                )
            )
        )

        CrossPayConfirmationScreen(navigation, viewModel, contentKey())
    }

    @Serializable
    data class Input(
        val wallet: Wallet,
        val tokenOut: Token,
        val recipient: String,
        @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
    )
}

@Composable
private fun CrossPayConfirmationScreen(
    navigation: HSNavigation,
    viewModel: CrossPayConfirmViewModel,
    screenContentKey: String,
) {
    val uiState = viewModel.uiState
    val view = LocalView.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val error = uiState.error
    if (error != null) {
        CrossPayConfirmationError(navigation, error)
        return
    }

    ConfirmTransactionScreen(
        title = stringResource(R.string.Send_Confirmation_Title),
        initialLoading = uiState.initialLoading,
        onClickBack = navigation::removeLastOrNull,
        onClickFeeSettings = if (uiState.hasSettings) {
            { navigation.slideFromRight(CrossPayTransactionSettingsPage(screenContentKey)) }
        } else {
            null
        },
        onClickNonceSettings = if (uiState.hasNonceSettings) {
            { navigation.slideFromRight(CrossPayTransactionNonceSettingsPage(screenContentKey)) }
        } else {
            null
        },
        buttonsSlot = {
            if (uiState.expired) {
                // The committed order is not honoured past its lifetime; Refresh commits a
                // fresh order in place — the same recovery the swap confirmation offers.
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Refresh),
                    onClick = viewModel::refresh,
                )
            } else {
                val sendAction = rememberAsyncAction()
                ButtonPrimaryYellow(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(if (sendAction.inProgress) R.string.Send_Sending else R.string.Send_Confirmation_Send_Button),
                    enabled = uiState.canSend && !sendAction.inProgress && !uiState.loading,
                    onClick = {
                        sendAction.run {
                            try {
                                viewModel.send()

                                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                                delay(1200)
                                navigation.removeLastUntil(CrossPayPage::class, true)
                            } catch (t: kotlinx.coroutines.CancellationException) {
                                // A back press during the success delay cancels this scope;
                                // showing the failure sheet then would invite a second send
                                // of a deposit that already broadcast.
                                throw t
                            } catch (t: Throwable) {
                                navigation.slideFromBottom(
                                    ErrorSheet(ErrorSheet.Input(errorText(context, t)))
                                )
                            }
                        }
                    },
                )
            }
        }
    ) {
        // What the recipient receives — the value the user entered, and contractual — then
        // the real recipient. The deposit address is never shown: it would confuse and it
        // leaks the rail.
        ConfirmationTopSection(
            token = uiState.tokenOut,
            amount = uiState.amountOut,
            coinMaxAllowedDecimals = uiState.tokenOut.decimals.coerceAtMost(8),
            rate = viewModel.rateOut,
            address = Address(uiState.recipient),
            contact = viewModel.contact,
        )
        VSpacer(height = 16.dp)
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
                .padding(vertical = 8.dp)
        ) {
            uiState.transactionFields.forEach {
                it.GetContent(navigation)
            }
            uiState.depositAmount?.let { depositAmount ->
                FeeRow(
                    title = stringResource(R.string.CrossPay_YouPay),
                    valueFiat = viewModel.rateIn?.let {
                        CurrencyValue(it.currency, it.value * depositAmount).getFormattedFull()
                    },
                    valueToken = CoinValue(uiState.tokenIn, depositAmount).getFormattedFull(),
                    onInfoClick = {
                        navigation.slideFromBottom(
                            SwapInfoSheet(
                                SwapInfoSheet.Input(
                                    context.getString(R.string.CrossPay_YouPay),
                                    context.getString(R.string.CrossPay_YouPay_Info),
                                )
                            )
                        )
                    },
                )
            }
            uiState.reservedAmount?.let { reserved ->
                FeeRow(
                    title = stringResource(R.string.PrivateSend_ReservedAmount),
                    valueFiat = viewModel.rateIn?.let {
                        CurrencyValue(it.currency, it.value * reserved).getFormattedFull()
                    },
                    valueToken = CoinValue(uiState.tokenIn, reserved).getFormattedFull(),
                    onInfoClick = {
                        navigation.slideFromBottom(
                            SwapInfoSheet(
                                SwapInfoSheet.Input(
                                    context.getString(R.string.PrivateSend_ReservedAmount),
                                    context.getString(R.string.PrivateSend_ReservedAmount_Info),
                                )
                            )
                        )
                    },
                )
            }
            uiState.estimatedTime?.let { estimatedTime ->
                QuoteInfoRow(
                    title = stringResource(R.string.PrivateSend_EstimatedTime),
                    value = formatDuration(estimatedTime).hs(ComposeAppTheme.colors.leah),
                    onInfoClick = {
                        navigation.slideFromBottom(
                            SwapInfoSheet(
                                SwapInfoSheet.Input(
                                    context.getString(R.string.PrivateSend_EstimatedTime),
                                    context.getString(R.string.Swap_EstimatedTimeDescription),
                                    R.drawable.ic_circle_clock_24,
                                )
                            )
                        )
                    },
                )
            }
            DataFieldFeeTemplate(
                navigation = navigation,
                primary = uiState.networkFee?.primary?.getFormattedPlain() ?: "---",
                secondary = uiState.networkFee?.secondary?.getFormattedPlain() ?: "---",
                title = stringResource(R.string.FeeSettings_NetworkFee),
                infoText = stringResource(R.string.FeeSettings_NetworkFee_Info),
            )
        }

        if (uiState.bufferUnknown) {
            VSpacer(16.dp)
            AlertCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                format = AlertFormat.Structured,
                type = AlertType.Caution,
                titleCustom = stringResource(R.string.CrossPay_Caution_Title),
                // The amount shown falls back to the deposit ceiling, so it is an upper bound.
                text = stringResource(R.string.PrivateSend_Caution_BufferUnknown),
            )
        }

        if (uiState.cautions.isNotEmpty()) {
            Cautions(cautions = uiState.cautions)
        }
    }
}

@Composable
private fun CrossPayConfirmationError(navigation: HSNavigation, error: Throwable) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Copy shares the SAME sanitized text the screen renders: the raw message can carry
    // host/route detail, and the clipboard is readable beyond this app.
    val message = errorText(context, error)

    ConfirmTransactionScreen(
        title = stringResource(R.string.Balance_Pay),
        onClickBack = navigation::removeLastOrNull,
        onClickFeeSettings = null,
        buttonsSlot = {
            ButtonPrimaryDefault(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Back),
                onClick = navigation::removeLastOrNull,
            )
        }
    ) {
        CardsErrorMessageDefault(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 64.dp, vertical = 32.dp),
            icon = painterResource(R.drawable.ic_warning_filled_24),
            iconTint = ComposeAppTheme.colors.grey,
            text = message,
            button4config = ButtonConfig(
                variant = ButtonVariant.Primary,
                style = ButtonStyle.Transparent,
                size = ButtonSize.Small,
                title = stringResource(R.string.Button_CopyError),
                onClick = {
                    TextHelper.copyText(message)
                }
            )
        )
    }
}

// Only authored reasons are rendered; anything unrecognised gets the generic copy — never
// raw server or transport text.
private fun errorText(context: android.content.Context, error: Throwable): String = when (error) {
    is CrossPayError.BelowMinimum -> context.getString(
        R.string.CrossPay_BelowMinimum,
        error.minimum.toPlainString(),
    )

    is CrossPayError.AboveMaximum -> context.getString(
        R.string.CrossPay_AboveMaximum,
        error.maximum.toPlainString(),
    )

    is CrossPayError.NoRoute -> context.getString(R.string.CrossPay_NoRoute)
    is CrossPayError.ProviderSuspended -> context.getString(R.string.CrossPay_ProviderSuspended)
    is CrossPayError.NetworkError -> context.getString(R.string.CrossPay_NetworkError)
    is CrossPayError.TokenUnsupported -> context.getString(R.string.CrossPay_TokenNotSupported)
    else -> context.getString(R.string.CrossPay_CommitFailed)
}

@Serializable
data class CrossPayTransactionSettingsPage(val parentScreenContentKey: String) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = navigation.viewModelForScreen<CrossPayConfirmViewModel>(parentScreenContentKey)
        viewModel.sendTransactionService.GetSettingsContent(navigation)
    }
}

@Serializable
data class CrossPayTransactionNonceSettingsPage(val parentScreenContentKey: String) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = navigation.viewModelForScreen<CrossPayConfirmViewModel>(parentScreenContentKey)
        viewModel.sendTransactionService.GetNonceSettingsContent(navigation)
    }
}
