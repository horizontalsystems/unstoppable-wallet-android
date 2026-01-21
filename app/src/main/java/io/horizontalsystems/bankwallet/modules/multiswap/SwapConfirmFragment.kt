package io.horizontalsystems.bankwallet.modules.multiswap

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.R.drawable.close_e_filled_24
import io.horizontalsystems.bankwallet.R.drawable.shield_check_filled_24
import io.horizontalsystems.bankwallet.R.drawable.warning_filled_24
import io.horizontalsystems.bankwallet.R.id.defenseSystemFeatureDialog
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.slideFromRightForResult
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.confirm.ErrorBottomSheet
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingsRecipientFragment
import io.horizontalsystems.bankwallet.modules.multiswap.settings.SwapSettingsSlippageFragment
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFee
import io.horizontalsystems.bankwallet.modules.premium.DefenseSystemFeatureDialog.Input
import io.horizontalsystems.bankwallet.modules.premium.PremiumFeature
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsImageCircle
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseAlertLevel
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseSystemMessage
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.util.Locale

class SwapConfirmFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapConfirmScreen(navController)
    }

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}

@Composable
fun SwapConfirmScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    val previousBackStackEntry = remember { navController.previousBackStackEntry }
    val swapViewModel = viewModel<SwapViewModel>(
        viewModelStoreOwner = previousBackStackEntry!!,
    )

    val currentQuote = remember { swapViewModel.getCurrentQuote() } ?: return

    val currentBackStackEntry = remember { navController.currentBackStackEntry }
    val viewModel = viewModel<SwapConfirmViewModel>(
        viewModelStoreOwner = currentBackStackEntry!!,
        initializer = SwapConfirmViewModel.init(currentQuote)
    )

    val uiState = viewModel.uiState

    val onClickSettings = if (uiState.hasSettings) {
        {
            navController.slideFromRight(R.id.swapTransactionSettings)
        }
    } else {
        null
    }

    val onClickNonceSettings = if (uiState.hasNonceSettings) {
        {
            navController.slideFromRight(R.id.swapTransactionNonceSettings)
        }
    } else {
        null
    }

    ConfirmTransactionScreen(
        title = stringResource(R.string.Swap_Confirm_Title),
        initialLoading = uiState.initialLoading,
        onClickBack = navController::popBackStack,
        onClickFeeSettings = onClickSettings,
        onClickNonceSettings = onClickNonceSettings,
        onClickSlippageSettings = {
            navController.slideFromRightForResult<SwapSettingsSlippageFragment.Result>(
                R.id.swapSettingsSlippage,
                SwapSettingsSlippageFragment.Input(uiState.slippage)
            ) {
                viewModel.setSlippage(it.slippage)
            }
        },
        onClickRecipientSettings = {
            navController.slideFromRightForResult<SwapSettingsRecipientFragment.Result>(
                R.id.swapSettingsRecipient,
                SwapSettingsRecipientFragment.Input(uiState.tokenIn, uiState.recipient)
            ) {
                viewModel.setRecipient(it.address)
            }
        },
        defenseSlot = {
            uiState.swapDefenseSystemMessage?.let { message ->
                val icon = when (message.level) {
                    DefenseAlertLevel.WARNING -> warning_filled_24
                    DefenseAlertLevel.DANGER -> warning_filled_24
                    DefenseAlertLevel.SAFE -> shield_check_filled_24
                    DefenseAlertLevel.IDLE -> close_e_filled_24
                }

                val onClick = message.requiredPaidAction?.let {
                    {
                        navController.slideFromBottom(
                            defenseSystemFeatureDialog,
                            Input(PremiumFeature.getFeature(paidAction = message.requiredPaidAction), true)
                        )
                    }
                }

                DefenseSystemMessage(
                    level = message.level,
                    title = message.title.getString(),
                    content = message.body.getString(),
                    icon = icon,
                    actionText = message.actionText?.getString(),
                    onClick = onClick
                )
            }
        },
        buttonsSlot = {
            if (uiState.loading) {
                ButtonPrimaryYellow(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Alert_Loading),
                    enabled = false,
                    onClick = { },
                )
            } else if (!uiState.validQuote) {
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Refresh),
                    onClick = {
                        viewModel.refresh()
                    },
                )
            } else {
                var buttonEnabled by remember { mutableStateOf(true) }
                var swapButtonTitle by remember { mutableIntStateOf(R.string.Swap) }
                ButtonPrimaryYellow(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(swapButtonTitle),
                    enabled = buttonEnabled,
                    onClick = {
                        coroutineScope.launch {
                            buttonEnabled = false
                            swapButtonTitle = R.string.Swap_Swapping

                            try {
                                viewModel.swap()

                                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                                delay(1200)
                                navController.setNavigationResultX(SwapConfirmFragment.Result(true))
                                navController.popBackStack()
                            } catch (t: Throwable) {
                                navController.slideFromBottom(R.id.errorBottomSheet, ErrorBottomSheet.Input(t.message ?: t.javaClass.simpleName))
                            }

                            swapButtonTitle = R.string.Swap
                            buttonEnabled = true
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
                PriceImpactField(uiState.priceImpact, uiState.priceImpactLevel, navController)
                uiState.amountOutMin?.let { amountOutMin ->
                    val infoTitle = stringResource(id = R.string.Swap_MinimumReceived)
                    val infoText = stringResource(id = R.string.Swap_MinimumReceivedDescription)
                    QuoteInfoRow(
                        title = stringResource(id = R.string.Swap_MinimumReceived),
                        value = CoinValue(uiState.tokenOut, amountOutMin).getFormattedFull()
                            .hs(ComposeAppTheme.colors.leah),
                        onInfoClick = {
                            navController.slideFromBottom(
                                R.id.swapInfoDialog,
                                SwapInfoDialog.Input(infoTitle, infoText)
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
                            navController.slideFromBottom(
                                R.id.swapInfoDialog,
                                SwapInfoDialog.Input(infoTitle, infoText)
                            )
                        }
                    )
                }
                uiState.quoteFields.forEach {
                    it.GetContent(navController)
                }
            }
            uiState.transactionFields.forEachIndexed { index, field ->
                field.GetContent(navController)
            }
            DataFieldFee(
                navController,
                uiState.networkFee?.primary?.getFormattedPlain() ?: "---",
                uiState.networkFee?.secondary?.getFormattedPlain() ?: "---"
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