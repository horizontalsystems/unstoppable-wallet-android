package cash.p.terminal.modules.multiswap

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.modules.confirm.ConfirmTransactionScreen
import cash.p.terminal.modules.evmfee.Cautions
import cash.p.terminal.modules.fee.FeeInfoSection
import cash.p.terminal.modules.multiswap.ui.SwapProviderField
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.modules.send.fee.NetworkFeeWarningOverlay
import cash.p.terminal.ui.compose.components.CoinImage
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.CellUniversal
import cash.p.terminal.ui_compose.components.HFillSpacer
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsImageCircle
import cash.p.terminal.ui_compose.components.HsSwitch
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.components.SectionUniversalLawrence
import cash.p.terminal.ui_compose.components.SnackbarDuration
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.caption_grey
import cash.p.terminal.ui_compose.components.subhead1_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.alternativeImageUrl
import cash.p.terminal.wallet.badge
import cash.p.terminal.wallet.imageUrl
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.entities.CurrencyValue
import kotlinx.coroutines.delay
import java.math.BigDecimal

@Composable
fun SwapConfirmScreen(
    fragmentNavController: NavController,
    swapNavController: NavController,
    swapViewModel: SwapViewModel,
    onOpenSettings: (() -> Unit)? = null,
) {
    val view = LocalView.current

    val currentQuote = remember { swapViewModel.getCurrentQuote() } ?: run {
        LaunchedEffect(Unit) {
            swapNavController.popBackStack()
        }
        return
    }
    val settings = remember { swapViewModel.getSettings() }

    val currentBackStackEntry = remember { swapNavController.currentBackStackEntry }
    val viewModel = viewModel<SwapConfirmViewModel>(
        viewModelStoreOwner = currentBackStackEntry!!,
        factory = SwapConfirmViewModel.provideFactory(currentQuote, settings, fragmentNavController)
    )

    val uiState = viewModel.uiState
    val sendResult = viewModel.sendResult

    // Handle send result UI - must be in Composable context for getString()
    when (sendResult) {
        SendResult.Sending -> {
            HudHelper.showInProcessMessage(
                view,
                R.string.Swap_Swapping,
                SnackbarDuration.INDEFINITE
            )
        }

        is SendResult.Sent -> {
            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
        }

        is SendResult.SentButQueued -> {
            HudHelper.showWarningMessage(view, R.string.send_success_queued)
        }

        is SendResult.Failed -> {
            HudHelper.showErrorMessage(view, sendResult.caution.getString())
        }

        null -> Unit
    }

    // Handle navigation after success
    LaunchedEffect(sendResult) {
        if (sendResult is SendResult.Sent || sendResult is SendResult.SentButQueued) {
            delay(1200)
            fragmentNavController.navigateUp()
        }
    }

    ConfirmTransactionScreen(
        onClickBack = swapNavController::navigateUp,
        onClickSettings = if (uiState.isAdvancedSettingsAvailable) {
            {
                onOpenSettings?.invoke()
            }
        } else {
            null
        },
        onClickClose = null,
        buttonsSlot = {
            if (uiState.loading) {
                ButtonPrimaryYellow(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Alert_Loading),
                    enabled = false,
                    onClick = { },
                )
                VSpacer(height = 12.dp)
                subhead1_leah(text = stringResource(id = R.string.SwapConfirm_FetchingFinalQuote))
            } else if (uiState.criticalError != null) {
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth(),
                    title = uiState.criticalError,
                    onClick = {
                        viewModel.refresh()
                    },
                )
                VSpacer(height = 12.dp)
            } else if (!uiState.validQuote) {
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Refresh),
                    onClick = viewModel::refresh
                )
                VSpacer(height = 12.dp)
                subhead1_leah(text = stringResource(id = R.string.SwapConfirm_QuoteIsInvalid))
            } else if (uiState.expired) {
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Refresh),
                    onClick = {
                        viewModel.refresh()
                    },
                )
                VSpacer(height = 12.dp)
                subhead1_leah(text = stringResource(id = R.string.SwapConfirm_QuoteExpired))
            } else {
                if (!viewModel.isSynced) {
                    TextImportantWarning(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.send_confirmation_syncing_warning)
                    )
                    VSpacer(height = 12.dp)
                }
                // Disable button during swap and navigation delay (allow retry only on Failed)
                val swapInProgress = sendResult != null && sendResult !is SendResult.Failed
                ButtonPrimaryYellow(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Swap),
                    enabled = viewModel.isSynced && !swapInProgress && uiState.amountOut != null &&
                            uiState.networkFee != null && uiState.feeCaution == null &&
                            uiState.cautions.none { it.type == CautionViewItem.Type.Error },
                    onClick = viewModel::onClickSendWithWarningCheck,
                )
                if (uiState.expiresIn != null) {
                    VSpacer(height = 12.dp)
                    subhead1_leah(text = "Quote expires in ${uiState.expiresIn}")
                }
            }
        }
    ) {
        SectionUniversalLawrence {
            TokenRow(
                token = uiState.tokenIn,
                amount = uiState.amountIn,
                fiatAmount = uiState.fiatAmountIn,
                currency = uiState.currency,
                borderTop = false,
                title = stringResource(R.string.Send_Confirmation_YouSend),
                amountColor = ComposeAppTheme.colors.leah,
            )
            TokenRow(
                token = uiState.tokenOut,
                amount = uiState.amountOut,
                fiatAmount = uiState.fiatAmountOut,
                currency = uiState.currency,
                title = stringResource(R.string.Swap_ToAmountTitle),
                amountColor = ComposeAppTheme.colors.remus,
            )
        }
        uiState.amountOut?.let { amountOut ->
            VSpacer(height = 16.dp)
            SectionUniversalLawrence {
                PriceField(
                    uiState.tokenIn,
                    uiState.tokenOut,
                    uiState.amountIn,
                    amountOut
                )
                PriceImpactField(
                    uiState.priceImpact,
                    uiState.priceImpactLevel,
                )
                uiState.amountOutMin?.let { amountOutMin ->
                    val subvalue = uiState.fiatAmountOutMin?.let { fiatAmountOutMin ->
                        CurrencyValue(uiState.currency, fiatAmountOutMin).getFormattedFull()
                    } ?: "---"

                    SwapInfoRow(
                        borderTop = true,
                        title = stringResource(id = R.string.Swap_MinimumReceived),
                        value = CoinValue(uiState.tokenOut, amountOutMin).getFormattedFull(),
                        subvalue = subvalue
                    )
                }
                swapViewModel.uiState.quote?.provider?.let { provider ->
                    SwapProviderField(
                        title = provider.title,
                        iconId = provider.icon
                    )
                }
                uiState.quoteFields.forEach {
                    it.GetContent(fragmentNavController, true)
                }
            }
        }

        val transactionFields = uiState.transactionFields
        if (transactionFields.isNotEmpty()) {
            VSpacer(height = 16.dp)
            SectionUniversalLawrence {
                transactionFields.forEachIndexed { index, field ->
                    field.GetContent(fragmentNavController, index != 0)
                }
            }
        }

        VSpacer(height = 16.dp)
        val swapUiState = swapViewModel.uiState
        val hasFeeError = uiState.feeCaution != null
        FeeInfoSection(
            tokenIn = uiState.tokenIn,
            displayBalance = swapUiState.displayBalance,
            balanceHidden = swapUiState.balanceHidden,
            feeToken = swapUiState.feeToken,
            feeCoinBalance = swapUiState.feeCoinBalance,
            feePrimary = uiState.networkFee?.primary?.getFormattedPlain() ?: "---",
            feeSecondary = uiState.networkFee?.secondary?.getFormattedPlain() ?: "---",
            insufficientFeeBalance = hasFeeError,
            onBalanceClicked = swapViewModel::toggleHideBalance,
        )

        if (uiState.mevProtectionAvailable) {
            VSpacer(16.dp)
            SectionUniversalLawrence {
                CellUniversal {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.ic_shield_24),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.jacob
                    )
                    HSpacer(width = 16.dp)
                    body_leah(text = stringResource(R.string.mev_protection))
                    HFillSpacer(minWidth = 8.dp)
                    HsSwitch(
                        checked = uiState.mevProtectionEnabled,
                        onCheckedChange = {
                            viewModel.toggleMevProtection(it)
                        }
                    )
                }
            }
        }


        if (uiState.cautions.isNotEmpty()) {
            Cautions(cautions = uiState.cautions)
        }
    }

    NetworkFeeWarningOverlay(
        feeWarningData = viewModel.feeWarningData,
        onConfirm = viewModel::onFeeWarningConfirmed,
        onCancel = viewModel::onFeeWarningCancelled,
    )
}

@Composable
private fun SwapInfoRow(
    borderTop: Boolean,
    title: String,
    value: String,
    subvalue: String? = null
) {
    CellUniversal(borderTop = borderTop) {
        subhead2_grey(text = title)
        HFillSpacer(minWidth = 16.dp)
        Column(horizontalAlignment = Alignment.End) {
            subhead2_leah(text = value)
            subvalue?.let {
                VSpacer(height = 1.dp)
                caption_grey(text = it)
            }
        }
    }
}

@Composable
fun TokenRow(
    token: Token,
    amount: BigDecimal?,
    fiatAmount: BigDecimal?,
    currency: Currency,
    borderTop: Boolean = true,
    title: String,
    amountColor: Color,
) = TokenRowPure(
    fiatAmount,
    borderTop,
    currency,
    title,
    amountColor,
    token.coin.imageUrl,
    token.coin.alternativeImageUrl,
    token.iconPlaceholder,
    token.badge,
    amount?.let { CoinValue(token, it).getFormattedFull() }
)

@Composable
fun TokenRowPure(
    fiatAmount: BigDecimal?,
    borderTop: Boolean = true,
    currency: Currency,
    title: String,
    amountColor: Color,
    imageUrl: String?,
    alternativeImageUrl: String?,
    imagePlaceholder: Int?,
    badge: String?,
    amountFormatted: String?,
) {
    CellUniversal(borderTop = borderTop) {
        HsImageCircle(
            modifier = Modifier.size(32.dp),
            url = imageUrl,
            alternativeUrl = alternativeImageUrl,
            placeholder = imagePlaceholder
        )
        HSpacer(width = 16.dp)
        Column {
            subhead2_leah(text = title)
            VSpacer(height = 1.dp)
            caption_grey(text = badge ?: stringResource(id = R.string.CoinPlatforms_Native))
        }
        HFillSpacer(minWidth = 16.dp)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = amountFormatted ?: "---",
                style = ComposeAppTheme.typography.subhead1,
                color = amountColor,
            )
            fiatAmount?.let {
                VSpacer(height = 1.dp)
                caption_grey(text = CurrencyValue(currency, fiatAmount).getFormattedFull())
            }
        }
    }
}

@Composable
fun TokenRowUnlimited(
    token: Token,
    borderTop: Boolean = true,
    title: String,
    amountColor: Color,
) {
    CellUniversal(borderTop = borderTop) {
        CoinImage(
            token = token,
            modifier = Modifier.size(32.dp)
        )
        HSpacer(width = 16.dp)
        Column {
            subhead2_leah(text = title)
            VSpacer(height = 1.dp)
            caption_grey(text = token.badge ?: stringResource(id = R.string.CoinPlatforms_Native))
        }
        HFillSpacer(minWidth = 16.dp)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "∞ ${token.coin.code}",
                style = ComposeAppTheme.typography.subhead1,
                color = amountColor,
            )
            VSpacer(height = 1.dp)
            caption_grey(text = stringResource(id = R.string.Transaction_Unlimited))
        }
    }
}
