package io.horizontalsystems.bankwallet.modules.multiswap

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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.FeeType
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFee
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFeeTemplate
import io.horizontalsystems.bankwallet.modules.premium.DefenseSystemFeatureDialog.Input
import io.horizontalsystems.bankwallet.modules.premium.PremiumFeature
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsImageCircle
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellSecondary
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseAlertLevel
import io.horizontalsystems.bankwallet.uiv3.components.message.DefenseSystemMessage
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

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
    val settings = remember { swapViewModel.getSettings() }

    val currentBackStackEntry = remember { navController.currentBackStackEntry }
    val viewModel = viewModel<SwapConfirmViewModel>(
        viewModelStoreOwner = currentBackStackEntry!!,
        initializer = SwapConfirmViewModel.init(currentQuote, settings)
    )

    val uiState = viewModel.uiState

    val onClickSettings = if (uiState.hasSettings) {
        {
            navController.slideFromRight(R.id.swapTransactionSettings)
        }
    } else {
        null
    }

    val onClickSlippageSettings = {
        navController.slideFromRight(R.id.swapTransactionSlippageSettings)
    }

    val onClickNonceSettings = if (uiState.hasNonceSettings) {
        {
            navController.slideFromRight(R.id.swapTransactionNonceSettings)
        }
    } else {
        null
    }

    val onClickRecipientSettings = if (true) {
        {
            navController.slideFromRight(R.id.swapTransactionRecipientSettings)
        }
    } else {
        null
    }

    ConfirmTransactionScreen(
        initialLoading = uiState.initialLoading,
        onClickBack = navController::popBackStack,
        onClickSettings = onClickSettings,
        onClickSlippageSettings = onClickSlippageSettings,
        onClickNonceSettings = onClickNonceSettings,
        onClickRecipientSettings = onClickRecipientSettings,
        onClickClose = null,
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
                VSpacer(height = 12.dp)
                subhead1_leah(text = stringResource(id = R.string.SwapConfirm_FetchingFinalQuote))
            } else if (!uiState.validQuote) {
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Refresh),
                    onClick = {
                        viewModel.refresh()
                    },
                )
                VSpacer(height = 12.dp)
                subhead1_leah(text = "Quote is invalid")
            } else {
                var buttonEnabled by remember { mutableStateOf(true) }
                ButtonPrimaryYellow(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Swap),
                    enabled = buttonEnabled,
                    onClick = {
                        coroutineScope.launch {
                            buttonEnabled = false
                            HudHelper.showInProcessMessage(
                                view,
                                R.string.Swap_Swapping,
                                SnackbarDuration.INDEFINITE
                            )

                            val result = try {
                                viewModel.swap()

                                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                                delay(1200)
                                SwapConfirmFragment.Result(true)
                            } catch (t: Throwable) {
                                HudHelper.showErrorMessage(view, t.javaClass.simpleName)
                                SwapConfirmFragment.Result(false)
                            }

                            buttonEnabled = true
                            navController.setNavigationResultX(result)
                            navController.popBackStack()
                        }
                    },
                )
            }
        }
    ) {
        Box() {
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
                    val subvalue = uiState.fiatAmountOutMin?.let { fiatAmountOutMin ->
                        CurrencyValue(uiState.currency, fiatAmountOutMin).getFormattedFull()
                    } ?: "---"
                    val infoTitle = stringResource(id = R.string.Swap_MinimumReceived)
                    val infoText = stringResource(id = R.string.Swap_MinimumReceivedDescription)

                    SwapInfoRow(
                        title = stringResource(id = R.string.Swap_MinimumReceived),
                        value = CoinValue(uiState.tokenOut, amountOutMin).getFormattedFull(),
                        subvalue = subvalue,
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
            uiState.extraFees.forEach { (type: FeeType, feeAmountData) ->
                DataFieldFeeTemplate(
                    navController = navController,
                    primary = feeAmountData.primary.getFormattedPlain(),
                    secondary = feeAmountData.secondary?.getFormattedPlain() ?: "---",
                    title = stringResource(type.stringResId),
                    infoText = null
                )
            }
            uiState.totalFee?.let { totalFee ->
                SwapInfoRow(
                    title = stringResource(id = R.string.Fee_Total),
                    value = totalFee.getFormattedFull(),
                )
            }
        }

        if (uiState.cautions.isNotEmpty()) {
            Cautions(cautions = uiState.cautions)
        }
    }
}

@Composable
private fun SwapInfoRow(
    title: String,
    value: String,
    subvalue: String? = null,
    onInfoClick: (() -> Unit)? = null,
) {
    CellSecondary(
        middle = {
            CellMiddleInfoTextIcon(
                text = title.hs(color = ComposeAppTheme.colors.grey),
                icon = painterResource(R.drawable.ic_info_filled_20),
                iconTint = ComposeAppTheme.colors.grey,
                onIconClick = onInfoClick
            )
        },
        right = {
            CellRightInfo(
                titleSubheadSb = value.hs(ComposeAppTheme.colors.leah),
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
                style = ComposeAppTheme.typography.subhead,
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
                style = ComposeAppTheme.typography.subhead,
                color = amountColor,
            )
            VSpacer(height = 1.dp)
            caption_grey(text = stringResource(id = R.string.Transaction_Unlimited))
        }
    }
}
