package io.horizontalsystems.bankwallet.modules.multiswap

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.os.Parcelable
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
import io.horizontalsystems.core.R
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.confirm.ErrorSheet
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingsRecipientPage
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingsSlippagePage
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapTransactionNonceSettingsPage
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapTransactionSettingsPage
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFee
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.modules.usersubscription.BuySubscriptionHavHostPage
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsImageCircle
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.rememberAsyncAction
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.AlertCard
import io.horizontalsystems.bankwallet.uiv3.components.AlertFormat
import io.horizontalsystems.bankwallet.uiv3.components.AlertType
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cards.CardsErrorMessageDefault
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightControlsSwitcher
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonConfig
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.bankwallet.uiv3.components.section.SectionHeader
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.Locale

@Serializable
data class SwapConfirmPage(val parentScreenContentKey: String) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        SwapConfirmScreen(navigation, parentScreenContentKey, contentKey())
    }

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}

@Composable
fun SwapConfirmScreen(
    navigation: HSNavigation,
    parentScreenContentKey: String,
    screenContentKey: String
) {
    val swapViewModel = navigation.viewModelForScreen<SwapViewModel>(parentScreenContentKey)
    val currentQuote = remember { swapViewModel.getCurrentQuote() } ?: return

    val viewModel = viewModel<SwapConfirmViewModel>(
        initializer = SwapConfirmViewModel.init(currentQuote)
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
                ButtonPrimaryDefault(
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
            text = stringResource(R.string.SwapError_FailedToFetchQuote),
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
        onClickRecipientSettings = navigation.slideFromRightForResult<SwapSettingsRecipientPage.Result>(
            { SwapSettingsRecipientPage(SwapSettingsRecipientPage.Input(uiState.tokenIn, uiState.recipient)) }
        ) {
            viewModel.setRecipient(it.address)
        },
        buttonsSlot = {
            if (!uiState.validQuote) {
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Refresh),
                    onClick = {
                        viewModel.refresh()
                    },
                )
            } else {
                val swapAction = rememberAsyncAction()
                ButtonPrimaryYellow(
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
                            } catch (t: Throwable) {
                                navigation.slideFromBottom(ErrorSheet(
                                    ErrorSheet.Input(t.message ?: t.javaClass.simpleName)
                                ))
                            }
                        }
                    },
                )
            }
        }
    ) {
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
        VSpacer(height = 16.dp)
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
                .padding(vertical = 8.dp)
        ) {
            uiState.amountOut?.let { amountOut ->
                PriceField(
                    tokenIn = uiState.tokenIn,
                    tokenOut = uiState.tokenOut,
                    amountIn = uiState.amountIn,
                    amountOut = amountOut,
                    statPage = StatPage.SwapConfirmation,
                )
                PriceImpactField(uiState.priceImpact, uiState.priceImpactLevel, navigation)
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
                    val infoTitle = stringResource(id = R.string.Swap_EstimatedTime)
                    val infoText = stringResource(id = R.string.Swap_EstimatedTimeDescription)
                    QuoteInfoRow(
                        title = stringResource(id = R.string.Swap_EstimatedTime),
                        value = "~${formatDuration(estimatedTime)}".hs(ComposeAppTheme.colors.leah),
                        onInfoClick = {
                            navigation.slideFromBottom(
                                SwapInfoSheet(SwapInfoSheet.Input(infoTitle, infoText))
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
                uiState.networkFee?.secondary?.getFormattedPlain() ?: "---"
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
                        confirmChange = {
                            if (!uiState.mevProtectionActionAllowed) {
                                navigation.slideFromBottom(BuySubscriptionHavHostPage)
                                false
                            } else {
                                true
                            }
                        },
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