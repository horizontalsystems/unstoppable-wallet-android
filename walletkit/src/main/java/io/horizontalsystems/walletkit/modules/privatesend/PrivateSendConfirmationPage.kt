package io.horizontalsystems.walletkit.modules.privatesend

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
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.walletkit.modules.confirm.ErrorSheet
import io.horizontalsystems.walletkit.modules.evmfee.Cautions
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.walletkit.modules.multiswap.SwapInfoSheet
import io.horizontalsystems.walletkit.modules.multiswap.formatDuration
import io.horizontalsystems.walletkit.modules.send.ConfirmationTopSection
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldFeeTemplate
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.serializers.HSScreenKClassSerializer
import io.horizontalsystems.walletkit.modules.send.SendPage
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.walletkit.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.rememberAsyncAction
import io.horizontalsystems.walletkit.ui.helpers.TextHelper
import io.horizontalsystems.walletkit.uiv3.components.cards.CardsErrorMessageDefault
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonConfig
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonSize
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.serializers.BigDecimalSerializer
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import kotlin.reflect.KClass

@Serializable
data class PrivateSendConfirmationPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        // The same toggle ViewModel the send screen filled in, resolved from the SendPage
        // store. The factory only matters after process death, where a fresh instance
        // carries no deposit params and the build degrades to service defaults — safe.
        val toggleViewModel = viewModel<PrivateSendViewModel>(
            viewModelStoreOwner = io.horizontalsystems.walletkit.modules.nav3.rememberChildViewModelStoreOwner(SendPage::class.simpleName!!),
            factory = PrivateSendViewModel.Factory(input.wallet.token),
        )

        val viewModel = viewModel<PrivateSendConfirmViewModel>(
            initializer = PrivateSendConfirmViewModel.init(
                PrivateSendRequest(
                    token = input.wallet.token,
                    recipient = input.recipient,
                    amountOut = input.amount,
                ),
                toggleViewModel.btcParams,
            )
        )

        PrivateSendConfirmationScreen(navigation, viewModel, contentKey(), input.sendEntryPointDestId)
    }

    @Serializable
    data class Input(
        val wallet: Wallet,
        val recipient: String,
        @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal,
        @Serializable(with = HSScreenKClassSerializer::class) val sendEntryPointDestId: KClass<out HSPage>?,
    )
}

@Composable
private fun PrivateSendConfirmationScreen(
    navigation: HSNavigation,
    viewModel: PrivateSendConfirmViewModel,
    screenContentKey: String,
    sendEntryPointDestId: KClass<out HSPage>?,
) {
    val uiState = viewModel.uiState
    val view = LocalView.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val closeUntilDestId = sendEntryPointDestId ?: SendPage::class

    val error = uiState.error
    if (error != null) {
        PrivateSendConfirmationError(navigation, error)
        return
    }

    ConfirmTransactionScreen(
        title = stringResource(R.string.Send_Confirmation_Title),
        initialLoading = uiState.initialLoading,
        onClickBack = navigation::removeLastOrNull,
        onClickFeeSettings = if (uiState.hasSettings) {
            { navigation.slideFromRight(PrivateSendTransactionSettingsPage(screenContentKey)) }
        } else {
            null
        },
        onClickNonceSettings = if (uiState.hasNonceSettings) {
            { navigation.slideFromRight(PrivateSendTransactionNonceSettingsPage(screenContentKey)) }
        } else {
            null
        },
        buttonsSlot = {
            if (uiState.expired) {
                // The committed order is not honoured past its lifetime, and it must not be
                // silently replaced under a live send button — the only way forward is a
                // fresh amount entry.
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.PrivateSend_QuoteExpired),
                    onClick = navigation::removeLastOrNull,
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
                                navigation.removeLastUntil(closeUntilDestId, true)
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
        // The same top section every regular send confirmation renders: what the recipient
        // receives — the value the user entered, and contractual — then the real recipient.
        // The deposit address is never shown: it would confuse and it leaks the rail.
        ConfirmationTopSection(
            token = uiState.token,
            amount = uiState.amountOut,
            coinMaxAllowedDecimals = uiState.token.decimals.coerceAtMost(8),
            rate = viewModel.coinRate,
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
            uiState.privateFee?.let { fee ->
                QuoteInfoRow(
                    title = stringResource(R.string.PrivateSend_Fee),
                    value = "~${CoinValue(uiState.token, fee).getFormattedFull()}".hs(ComposeAppTheme.colors.leah),
                    onInfoClick = {
                        navigation.slideFromBottom(
                            SwapInfoSheet(
                                SwapInfoSheet.Input(
                                    context.getString(R.string.PrivateSend_Fee),
                                    context.getString(R.string.PrivateSend_Fee_Info),
                                    R.drawable.ic_incognito_24,
                                )
                            )
                        )
                    },
                )
            }
            uiState.reservedAmount?.let { reserved ->
                QuoteInfoRow(
                    title = stringResource(R.string.PrivateSend_ReservedAmount),
                    value = "~${CoinValue(uiState.token, reserved).getFormattedFull()}".hs(ComposeAppTheme.colors.leah),
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
            io.horizontalsystems.walletkit.uiv3.components.AlertCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                format = io.horizontalsystems.walletkit.uiv3.components.AlertFormat.Structured,
                type = io.horizontalsystems.walletkit.uiv3.components.AlertType.Caution,
                titleCustom = stringResource(R.string.PrivateSend_Caution_Title),
                // The fee shown falls back to the deposit ceiling, so it is an upper bound.
                text = stringResource(R.string.PrivateSend_Caution_BufferUnknown),
            )
        }

        if (uiState.cautions.isNotEmpty()) {
            Cautions(cautions = uiState.cautions)
        }
    }
}

@Composable
private fun PrivateSendConfirmationError(navigation: HSNavigation, error: Throwable) {
    val context = androidx.compose.ui.platform.LocalContext.current

    ConfirmTransactionScreen(
        title = stringResource(R.string.PrivateSend_Toggle_Title),
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
            text = errorText(context, error),
            button4config = ButtonConfig(
                variant = ButtonVariant.Primary,
                style = ButtonStyle.Transparent,
                size = ButtonSize.Small,
                title = stringResource(R.string.Button_CopyError),
                onClick = {
                    TextHelper.copyText(error.message ?: error.javaClass.simpleName)
                }
            )
        )
    }
}

// Only authored reasons are rendered; anything unrecognised gets the generic copy — never
// raw server or transport text, which can leak hosts and route detail for the one feature
// whose point is unlinkability.
private fun errorText(context: android.content.Context, error: Throwable): String = when (error) {
    is PrivateSendError.BelowMinimum -> context.getString(
        R.string.PrivateSend_Caution_BelowMinimum,
        error.minimum.toPlainString(),
    )

    is PrivateSendError.AboveMaximum -> context.getString(
        R.string.PrivateSend_Caution_AboveMaximum,
        error.maximum.toPlainString(),
    )

    is PrivateSendError.NoRoute -> context.getString(R.string.PrivateSend_Caution_NoRoute)
    is PrivateSendError.ExactOutputUnsupported -> context.getString(R.string.PrivateSend_Caution_ExactOutputUnsupported)
    is PrivateSendError.ProviderSuspended -> context.getString(R.string.PrivateSend_Caution_ProviderSuspended)
    is PrivateSendError.NetworkError -> context.getString(R.string.PrivateSend_Caution_NetworkError)
    is PrivateSendError.TokenUnsupported -> context.getString(R.string.PrivateSend_Caution_TokenUnsupported)
    is PrivateSendError.AttachmentUnsupported -> context.getString(R.string.PrivateSend_Caution_AttachmentUnsupported)
    else -> context.getString(R.string.PrivateSend_CommitFailed)
}

@Serializable
data class PrivateSendTransactionSettingsPage(val parentScreenContentKey: String) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = navigation.viewModelForScreen<PrivateSendConfirmViewModel>(parentScreenContentKey)
        viewModel.sendTransactionService.GetSettingsContent(navigation)
    }
}

@Serializable
data class PrivateSendTransactionNonceSettingsPage(val parentScreenContentKey: String) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = navigation.viewModelForScreen<PrivateSendConfirmViewModel>(parentScreenContentKey)
        viewModel.sendTransactionService.GetNonceSettingsContent(navigation)
    }
}
