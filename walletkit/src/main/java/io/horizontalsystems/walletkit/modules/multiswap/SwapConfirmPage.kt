package io.horizontalsystems.walletkit.modules.multiswap

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.WatchAccountException
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.core.alternativeImageUrl
import io.horizontalsystems.walletkit.core.badge
import io.horizontalsystems.walletkit.core.iconPlaceholder
import io.horizontalsystems.walletkit.core.imageUrl
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.entities.CurrencyValue
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.walletkit.modules.confirm.ErrorSheet
import io.horizontalsystems.walletkit.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.walletkit.modules.evmfee.Cautions
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapProviderType
import io.horizontalsystems.walletkit.modules.multiswap.settings.SwapSettingsRecipientPage
import io.horizontalsystems.walletkit.modules.multiswap.settings.SwapSettingsSlippagePage
import io.horizontalsystems.walletkit.modules.multiswap.settings.SwapTransactionNonceSettingsPage
import io.horizontalsystems.walletkit.modules.multiswap.settings.SwapTransactionSettingsPage
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataFieldFee
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.nav3.LocalResultEventBus
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.HsImageCircle
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.rememberAsyncAction
import io.horizontalsystems.walletkit.ui.helpers.TextHelper
import io.horizontalsystems.walletkit.uiv3.components.AlertCard
import io.horizontalsystems.walletkit.uiv3.components.AlertFormat
import io.horizontalsystems.walletkit.uiv3.components.AlertType
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.cards.CardsErrorMessageDefault
import io.horizontalsystems.walletkit.modules.multiswap.history.DividerWithArrow
import io.horizontalsystems.walletkit.modules.multiswap.history.shortenAddress
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_grey
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_jacob
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_leah
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightControlsButtonText
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightControlsSwitcher
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellSecondary
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonConfig
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonSize
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.info.TextBlock
import io.horizontalsystems.walletkit.uiv3.components.section.SectionHeader
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.Locale
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton

@Serializable
data class SwapConfirmPage(
    val parentScreenContentKey: String,
    // serialized with the page so an external recipient survives process recreation
    val initialRecipient: Address? = null,
    // A split swap's recipients with their shares; null confirms a single swap
    val splitRecipients: List<SplitSwapRecipient>? = null,
    // Which of the swap screen's tabs the quote belongs to; null is the Standard tab
    val swapViewModelKey: String? = null,
) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        SwapConfirmScreen(navigation, parentScreenContentKey, contentKey(), initialRecipient, splitRecipients, swapViewModelKey)
    }

    data class Result(val success: Boolean)
}

@Composable
fun SwapConfirmScreen(
    navigation: HSNavigation,
    parentScreenContentKey: String,
    screenContentKey: String,
    pageRecipient: Address? = null,
    splitRecipients: List<SplitSwapRecipient>? = null,
    swapViewModelKey: String? = null,
) {
    val swapViewModel = navigation.viewModelForScreen<SwapViewModel>(parentScreenContentKey, key = swapViewModelKey)
    val currentQuote = remember { swapViewModel.getCurrentQuote() } ?: return
    val initialRecipient = remember { pageRecipient ?: swapViewModel.externalRecipient }

    val viewModel = viewModel<SwapConfirmViewModel>(
        initializer = SwapConfirmViewModel.init(currentQuote, initialRecipient, splitRecipients)
    )

    val uiState = viewModel.uiState

    if (uiState.error != null) {
        SwapConfirmError(navigation, viewModel, uiState, uiState.error)
    } else {
        SwapConfirmInternal(navigation, viewModel, uiState, screenContentKey)
    }
}

@Composable
private fun SwapConfirmError(
    navigation: HSNavigation,
    viewModel: SwapConfirmViewModel,
    uiState: SwapConfirmUiState,
    error: Throwable
) {
    HSScaffold(
        title = stringResource(R.string.Swap_Confirm_Title),
        onBack = navigation::removeLastOrNull,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Settings_Title),
                icon = R.drawable.manage_24,
                enabled = false,
                onClick = {}
            )
        ),
        bottomBar = {
            ButtonsGroupWithShade {
                HSButton(
                    variant = ButtonVariant.Secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.Button_Refresh),
                    onClick = viewModel::refresh,
                    enabled = !uiState.loading
                )
            }
        }
    ) {
        CardsErrorMessageDefault(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 64.dp),
            icon = painterResource(R.drawable.ic_warning_filled_24),
            iconTint = ComposeAppTheme.colors.grey,
            // A refused quote is not a failure to reach the provider: say which wallet to
            // change, and let Copy Error carry the same thing rather than a class name.
            text = if (error is WatchAccountException) {
                stringResource(R.string.Hud_Text_ChangeWallet)
            } else {
                stringResource(R.string.SwapError_FailedToFetchQuote)
            },
            button4config = ButtonConfig(
                variant = ButtonVariant.Primary,
                style = ButtonStyle.Transparent,
                size = ButtonSize.Small,
                title = stringResource(R.string.Button_CopyError),
                onClick = {
                    TextHelper.copyText(
                        when (error) {
                            is WatchAccountException ->
                                Translator.getString(R.string.Hud_Text_ChangeWallet)

                            else -> error.message ?: error.javaClass.simpleName
                        }
                    )
                }
            )
        )
    }
}

@Composable
private fun SwapConfirmInternal(
    navigation: HSNavigation,
    viewModel: SwapConfirmViewModel,
    uiState: SwapConfirmUiState,
    screenContentKey: String,
) {
    val resultEventBus = LocalResultEventBus.current
    val view = LocalView.current

    val onClickSettings = if (uiState.hasSettings) {
        {
            navigation.slideFromRight(SwapTransactionSettingsPage(screenContentKey))
        }
    } else {
        null
    }

    val onClickNonceSettings = if (uiState.hasNonceSettings) {
        {
            navigation.slideFromRight(SwapTransactionNonceSettingsPage(screenContentKey))
        }
    } else {
        null
    }

    ConfirmTransactionScreen(
        title = stringResource(R.string.Swap_Confirm_Title),
        initialLoading = uiState.initialLoading,
        onClickBack = navigation::removeLastOrNull,
        onClickFeeSettings = onClickSettings,
        onClickNonceSettings = onClickNonceSettings,
        onClickSlippageSettings = uiState.slippage?.let { slippage ->
            navigation.slideFromRightForResult<SwapSettingsSlippagePage.Result>(
                { SwapSettingsSlippagePage(SwapSettingsSlippagePage.Input(slippage)) }
            ) {
                viewModel.setSlippage(it.slippage)
            }
        },
        // A split's recipients were fixed with their shares before this screen.
        onClickRecipientSettings = if (viewModel.isSplit) null else navigation.slideFromRightForResult<SwapSettingsRecipientPage.Result>(
            {
                // the recipient receives tokenOut, so the address is validated against
                // its blockchain; a mandatory external recipient can't be removed
                SwapSettingsRecipientPage(
                    SwapSettingsRecipientPage.Input(
                        uiState.tokenOut,
                        uiState.recipient,
                        allowRemoval = !uiState.recipientRequired,
                        zcashTransparentOnly = viewModel.zcashTransparentRecipientOnly,
                    )
                )
            }
        ) {
            viewModel.setRecipient(it.address)
        },
        buttonsSlot = {
            if (!uiState.validQuote) {
                HSButton(
                    variant = ButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Refresh),
                    onClick = {
                        viewModel.refresh()
                    },
                )
            } else {
                val swapAction = rememberAsyncAction()
                HSButton(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(if (swapAction.inProgress) R.string.Swap_Swapping else R.string.Swap),
                    enabled = !swapAction.inProgress && !uiState.loading,
                    onClick = {
                        swapAction.run {
                            try {
                                viewModel.swap()

                                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                                delay(1200)
                                resultEventBus.sendResult(SwapConfirmPage.Result(true))
                                navigation.removeLastOrNull()
                            } catch (t: kotlinx.coroutines.CancellationException) {
                                // A back press during the success delay cancels this scope;
                                // showing the failure sheet then would invite a second send
                                // of a deposit that already broadcast.
                                throw t
                            } catch (t: SwapCommitFailedException) {
                                // No deposit was sent; a fresh quote is already loading, so the
                                // reason is a passing notice rather than an error sheet.
                                HudHelper.showErrorMessage(view, t.detail ?: Translator.getString(t.failure.messageRes))
                            } catch (t: Throwable) {
                                val message = when (t) {
                                    is WatchAccountException ->
                                        Translator.getString(R.string.Hud_Text_ChangeWallet)

                                    else -> t.message ?: t.javaClass.simpleName
                                }
                                navigation.slideFromBottom(ErrorSheet(ErrorSheet.Input(message)))
                            }
                        }
                    },
                )
            }
        }
    ) {
        val splitOutputs = uiState.splitOutputs
        if (splitOutputs != null) {
            SplitSwapFlowCard(uiState, splitOutputs)
        } else {
            Box {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ComposeAppTheme.colors.lawrence)
                ) {
                    TokenRow(
                        token = uiState.tokenIn,
                        amount = uiState.amountIn,
                        fiatAmount = uiState.fiatAmountIn,
                        currency = uiState.currency,
                    )
                    HsDivider()
                    TokenRow(
                        token = uiState.tokenOut,
                        amount = uiState.amountOut,
                        fiatAmount = uiState.fiatAmountOut,
                        currency = uiState.currency,
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_down_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(ComposeAppTheme.colors.lawrence)
                )
            }
        }
        VSpacer(height = 16.dp)
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
                .padding(vertical = 8.dp)
        ) {
            uiState.amountOut?.let { amountOut ->
                // A split's outputs are priced separately, so a single price and its impact
                // would describe none of them.
                if (uiState.splitOutputs == null) {
                    PriceField(
                        tokenIn = uiState.tokenIn,
                        tokenOut = uiState.tokenOut,
                        amountIn = uiState.amountIn,
                        amountOut = amountOut,
                        statPage = StatPage.SwapConfirmation,
                    )
                    PriceImpactField(uiState.priceImpact, uiState.priceImpactLevel, navigation)
                }
                uiState.amountOutMin?.let { amountOutMin ->
                    val infoTitle = stringResource(id = R.string.Swap_MinimumReceived)
                    val infoText = stringResource(id = R.string.Swap_MinimumReceivedDescription)
                    QuoteInfoRow(
                        title = stringResource(id = R.string.Swap_MinimumReceived),
                        value = CoinValue(uiState.tokenOut, amountOutMin).getFormattedFull()
                            .hs(ComposeAppTheme.colors.leah),
                        onInfoClick = {
                            navigation.slideFromBottom(
                                SwapInfoSheet(SwapInfoSheet.Input(infoTitle, infoText))
                            )
                        }
                    )
                }
                uiState.estimatedTime?.let { estimatedTime ->
                    val infoTitle = stringResource(id = R.string.Swap_SwapTime)
                    val infoText = stringResource(id = R.string.Swap_EstimatedTimeDescription)
                    val timeText = when (uiState.providerType) {
                        SwapProviderType.CEX -> formatSwapTimeRange(estimatedTime)
                        SwapProviderType.DEX -> "~${formatDuration(estimatedTime)}"
                    }
                    QuoteInfoRow(
                        title = stringResource(id = R.string.Swap_SwapTime),
                        value = timeText.hs(if (uiState.splitOutputs != null) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.leah),
                        onInfoClick = {
                            navigation.slideFromBottom(
                                SwapInfoSheet(SwapInfoSheet.Input(infoTitle, infoText, R.drawable.ic_circle_clock_24))
                            )
                        }
                    )
                }
                uiState.quoteFields.forEach {
                    it.GetContent(navigation)
                }
            }
            uiState.transactionFields.forEachIndexed { index, field ->
                field.GetContent(navigation)
            }
            DataFieldFee(
                navigation,
                uiState.networkFee?.primary?.getFormattedPlain() ?: "---",
                uiState.networkFee?.secondary?.getFormattedPlain()
            )
        }

        val defenseMessage = uiState.swapDefenseSystemMessage
        if (defenseMessage != null &&
            (defenseMessage.level == DefenseAlertLevel.WARNING || defenseMessage.level == DefenseAlertLevel.DANGER)
        ) {
            val alertType = if (defenseMessage.level == DefenseAlertLevel.DANGER) AlertType.Critical else AlertType.Caution
            VSpacer(16.dp)
            AlertCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                format = AlertFormat.Structured,
                type = alertType,
                titleCustom = defenseMessage.title.getString(),
                text = defenseMessage.body.getString(),
            )
        }

        if (uiState.supportsMevProtection) {
            VSpacer(16.dp)
            SectionHeader(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(R.string.SwapConfirm_SwapProtection),
                icon = R.drawable.ic_defense_shield_20
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ComposeAppTheme.colors.lawrence)
                    .border(0.5.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    CellMiddleInfo(title = stringResource(R.string.SwapConfirm_Mev).hs)
                }
                Box(
                    modifier = Modifier.widthIn(max = 200.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    CellRightControlsSwitcher(
                        checked = uiState.mevProtectionEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setMevProtectionEnabled(enabled)
                        }
                    )
                }
            }

            TextBlock(
                text = stringResource(R.string.SwapConfirm_MevDescription),
            )
        }

        if (uiState.cautions.isNotEmpty()) {
            Cautions(cautions = uiState.cautions)
        }
    }
}

// The deposit, then every output with its share and the address it is paid to: what the
// user confirms is where each part of the deposit ends up.
@Composable
private fun SplitSwapFlowCard(uiState: SwapConfirmUiState, outputs: List<SplitSwapOutput>) {
    val view = LocalView.current
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        TokenRow(
            token = uiState.tokenIn,
            amount = uiState.amountIn,
            fiatAmount = uiState.fiatAmountIn,
            currency = uiState.currency,
        )
        outputs.forEachIndexed { index, output ->
            if (index == 0) DividerWithArrow() else HsDivider()
            SplitOutputRow(
                token = uiState.tokenOut,
                percent = output.percent,
                amount = output.amountOut,
                fiatAmount = uiState.splitOutputFiatAmounts?.getOrNull(index),
                currency = uiState.currency,
            )
            SplitAddressRow(
                title = stringResource(R.string.SplitSwap_Address, index + 1),
                address = output.address,
                view = view,
            )
        }
    }
}

/** One output of a split: the bought token with this output's share beside its code. */
@Composable
fun SplitOutputRow(
    token: Token,
    percent: Int,
    amount: BigDecimal?,
    fiatAmount: BigDecimal?,
    currency: Currency,
) = SplitOutputRowPure(
    percent = percent,
    amountFormatted = amount?.let { CoinValue(token, it).getFormatted() },
    fiatAmount = fiatAmount,
    currency = currency,
    code = token.coin.code,
    imageUrl = token.coin.imageUrl,
    alternativeImageUrl = token.coin.alternativeImageUrl,
    imagePlaceholder = token.iconPlaceholder,
    badge = token.badge,
)

@Composable
fun SplitOutputRowPure(
    percent: Int,
    amountFormatted: String?,
    fiatAmount: BigDecimal?,
    currency: Currency,
    code: String,
    imageUrl: String?,
    alternativeImageUrl: String?,
    imagePlaceholder: Int?,
    badge: String?,
) {
    CellPrimary(
        left = {
            HsImageCircle(
                modifier = Modifier.size(32.dp),
                url = imageUrl,
                alternativeUrl = alternativeImageUrl,
                placeholder = imagePlaceholder
            )
        },
        middle = {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    subhead2_leah(text = code)
                    subhead2_jacob(text = "$percent%")
                }
                subhead2_grey(text = badge ?: stringResource(id = R.string.CoinPlatforms_Native))
            }
        },
        right = {
            CellRightInfo(
                eyebrow = (amountFormatted ?: "---").hs(ComposeAppTheme.colors.leah),
                subtitle = fiatAmount?.let { CurrencyValue(currency, it).getFormattedFull() }?.hs,
            )
        }
    )
}

/** A titled address row with a copy action, for the destination of one split output. */
@Composable
fun SplitAddressRow(title: String, address: String, view: android.view.View) {
    val leah = ComposeAppTheme.colors.leah
    CellSecondary(
        middle = {
            CellMiddleInfoTextIcon(text = title.hs)
        },
        right = {
            CellRightControlsButtonText(
                subtitle = address.shortenAddress().hs(color = leah),
                icon = painterResource(R.drawable.copy_filled_24),
                iconTint = leah,
                onIconClick = {
                    TextHelper.copyText(address)
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                },
            )
        },
    )
}

@Composable
fun TokenRow(
    token: Token,
    amount: BigDecimal?,
    fiatAmount: BigDecimal?,
    currency: Currency,
) = TokenRowPure(
    fiatAmount,
    currency,
    token.coin.code,
    token.coin.imageUrl,
    token.coin.alternativeImageUrl,
    token.iconPlaceholder,
    token.badge,
    amount?.let { CoinValue(token, it).getFormatted() }
)

@Composable
fun TokenRowPure(
    fiatAmount: BigDecimal?,
    currency: Currency,
    title: String,
    imageUrl: String?,
    alternativeImageUrl: String?,
    imagePlaceholder: Int?,
    badge: String?,
    amountFormatted: String?,
) {
    CellPrimary(
        left = {
            HsImageCircle(
                modifier = Modifier.size(32.dp),
                url = imageUrl,
                alternativeUrl = alternativeImageUrl,
                placeholder = imagePlaceholder
            )
        },
        middle = {
            CellMiddleInfo(
                eyebrow = title.hs(color = ComposeAppTheme.colors.leah),
                subtitle = (badge ?: stringResource(id =R.string.CoinPlatforms_Native)).hs
            )
        },
        right = {
            CellRightInfo(
                eyebrow = amountFormatted?.hs(ComposeAppTheme.colors.leah) ?: "---".hs(ComposeAppTheme.colors.leah),
                subtitle = fiatAmount?.let {
                    CurrencyValue(currency, fiatAmount).getFormattedFull()
                }?.hs
            )
        }
    )
}

fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val measures = mutableListOf<Measure>()
    if (hours > 0) measures.add(Measure(hours, MeasureUnit.HOUR))
    if (minutes > 0) measures.add(Measure(minutes, MeasureUnit.MINUTE))
    // Include seconds if they exist, or if the total duration is 0
    if (seconds > 0 || measures.isEmpty()) measures.add(Measure(seconds, MeasureUnit.SECOND))

    val fmt = MeasureFormat.getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.SHORT)

    // formatMeasures takes an array and joins them localized (e.g., "2 mins 25 secs")
    return fmt.formatMeasures(*measures.toTypedArray())
}
val SwapCommitFailure.messageRes: Int
    get() = when (this) {
        SwapCommitFailure.RateChanged -> R.string.SwapConfirm_CommitRateChanged
        SwapCommitFailure.RefreshRequired -> R.string.SwapConfirm_CommitRefreshRequired
        SwapCommitFailure.Failed -> R.string.SwapConfirm_CommitFailed
    }
