package io.horizontalsystems.bankwallet.modules.multiswap

import android.os.Parcelable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsInfoDialog
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
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

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.Swap_Confirm_Title),
                navigationIcon = {
                    HsBackButton(onClick = navController::popBackStack)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Settings_Title),
                        icon = R.drawable.ic_manage_2_24,
                        onClick = {
                            navController.slideFromRight(R.id.swapTransactionSettings)

                            stat(page = StatPage.SwapConfirmation, event = StatEvent.Open(StatPage.SwapSettings))
                        }
                    )
                ),
            )
        },
        bottomBar = {
            ButtonsGroupWithShade {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (uiState.loading) {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            title = stringResource(R.string.Alert_Loading),
                            enabled = false,
                            onClick = { },
                        )
                        VSpacer(height = 12.dp)
                        subhead1_leah(text = stringResource(id = R.string.SwapConfirm_FetchingFinalQuote))
                    } else if (!uiState.validQuote) {
                        ButtonPrimaryDefault(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            title = stringResource(R.string.Button_Refresh),
                            onClick = {
                                viewModel.refresh()
                            },
                        )
                        VSpacer(height = 12.dp)
                        subhead1_leah(text = "Quote is invalid")
                    } else if (uiState.expired) {
                        ButtonPrimaryDefault(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            title = stringResource(R.string.Button_Refresh),
                            onClick = {
                                viewModel.refresh()
                            },
                        )
                        VSpacer(height = 12.dp)
                        subhead1_leah(text = stringResource(id = R.string.SwapConfirm_QuoteExpired))
                    } else {
                        var buttonEnabled by remember { mutableStateOf(true) }
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            title = stringResource(R.string.Swap),
                            enabled = buttonEnabled,
                            onClick = {
                                coroutineScope.launch {
                                    buttonEnabled = false
                                    HudHelper.showInProcessMessage(view, R.string.Swap_Swapping, SnackbarDuration.INDEFINITE)

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
                        VSpacer(height = 12.dp)
                        subhead1_leah(text = "Quote expires in ${uiState.expiresIn}")
                    }
                }
            }
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            VSpacer(height = 12.dp)
            SectionUniversalLawrence {
                TokenRow(uiState.tokenIn, uiState.amountIn, uiState.fiatAmountIn, uiState.currency, TokenRowType.In, false,)
                TokenRow(uiState.tokenOut, uiState.amountOut, uiState.fiatAmountOut, uiState.currency, TokenRowType.Out)
            }
            uiState.amountOut?.let { amountOut ->
                VSpacer(height = 16.dp)
                SectionUniversalLawrence {
                    PriceField(uiState.tokenIn, uiState.tokenOut, uiState.amountIn, amountOut, StatPage.SwapConfirmation)
                    PriceImpactField(uiState.priceImpact, uiState.priceImpactLevel, navController)
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
                    uiState.quoteFields.forEach {
                        it.GetContent(navController, true)
                    }
                }
            }

            val transactionFields = uiState.transactionFields
            if (transactionFields.isNotEmpty()) {
                VSpacer(height = 16.dp)
                SectionUniversalLawrence {
                    transactionFields.forEachIndexed { index, field ->
                        field.GetContent(navController, index != 0)
                    }
                }
            }

            VSpacer(height = 16.dp)
            SectionUniversalLawrence {
                QuoteInfoRow(
                    title = {
                        val title = stringResource(id = R.string.FeeSettings_NetworkFee)
                        val infoText = stringResource(id = R.string.FeeSettings_NetworkFee_Info)

                        subhead2_grey(text = title)

                        Image(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clickable(
                                    onClick = {
                                        navController.slideFromBottom(
                                            R.id.feeSettingsInfoDialog,
                                            FeeSettingsInfoDialog.Input(title, infoText)
                                        )
                                    },
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                )
                            ,
                            painter = painterResource(id = R.drawable.ic_info_20),
                            contentDescription = ""
                        )

                    },
                    value = {
                        val primary = uiState.networkFee?.primary?.getFormattedPlain() ?: "---"
                        val secondary = uiState.networkFee?.secondary?.getFormattedPlain() ?: "---"

                        Column(horizontalAlignment = Alignment.End) {
                            subhead2_leah(text = primary)
                            VSpacer(height = 1.dp)
                            subhead2_grey(text = secondary)
                        }
                    }
                )
            }

            if (uiState.cautions.isNotEmpty()) {
                Cautions(cautions = uiState.cautions)
            }

            VSpacer(height = 32.dp)
        }
    }
}

@Composable
private fun SwapInfoRow(borderTop: Boolean, title: String, value: String, subvalue: String? = null) {
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

enum class TokenRowType {
    In, Out;
}

@Composable
private fun TokenRow(
    token: Token,
    amount: BigDecimal?,
    fiatAmount: BigDecimal?,
    currency: Currency,
    type: TokenRowType,
    borderTop: Boolean = true,
) {
    CellUniversal(borderTop = borderTop) {
        CoinImage(
            iconUrl = token.coin.imageUrl,
            placeholder = token.iconPlaceholder,
            modifier = Modifier.size(32.dp)
        )
        HSpacer(width = 16.dp)
        Column {
            val title = when (type) {
                TokenRowType.In -> stringResource(R.string.Send_Confirmation_YouSend)
                TokenRowType.Out -> stringResource(R.string.Swap_ToAmountTitle)
            }

            subhead2_leah(text = title)
            VSpacer(height = 1.dp)
            caption_grey(text = token.badge ?: stringResource(id = R.string.CoinPlatforms_Native))
        }
        HFillSpacer(minWidth = 16.dp)
        Column(horizontalAlignment = Alignment.End) {
            val color = when (type) {
                TokenRowType.In -> ComposeAppTheme.colors.leah
                TokenRowType.Out -> ComposeAppTheme.colors.remus
            }
            Text(
                text = amount?.let { CoinValue(token, it).getFormattedFull() } ?: "---",
                style = ComposeAppTheme.typography.subhead1,
                color = color,
            )
            fiatAmount?.let {
                VSpacer(height = 1.dp)
                caption_grey(text = CurrencyValue(currency, fiatAmount).getFormattedFull())
            }
        }
    }
}
