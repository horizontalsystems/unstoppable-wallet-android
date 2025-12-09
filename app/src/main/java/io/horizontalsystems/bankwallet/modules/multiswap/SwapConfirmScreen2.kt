package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.paidAction
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.FeeType
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFee
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFeeTemplate
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.PremiumHeader
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionPremiumUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.subscriptions.core.LossProtection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SwapConfirmScreen2(
    navController: NavController,
    currentQuote: SwapProviderQuote,
    settings: Map<String, Any?>
) {
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

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

    ConfirmTransactionScreen(
        onClickBack = navController::popBackStack,
        onClickSettings = onClickSettings,
        onClickClose = null,
        buttonsSlot = {
            if (uiState.loading) {
                ButtonPrimaryYellow(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    title = stringResource(R.string.Alert_Loading),
                    enabled = false,
                    onClick = { },
                )
                VSpacer(height = 12.dp)
                subhead1_leah(text = stringResource(id = R.string.SwapConfirm_FetchingFinalQuote))
            } else if (!uiState.validQuote) {
                ButtonPrimaryDefault(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    title = stringResource(R.string.Button_Refresh),
                    onClick = {
                        viewModel.refresh()
                    },
                )
                VSpacer(height = 12.dp)
                subhead1_leah(text = "Quote is invalid")
            } else if (uiState.expired) {
                ButtonPrimaryDefault(
                    modifier = Modifier.Companion.fillMaxWidth(),
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
                    modifier = Modifier.Companion.fillMaxWidth(),
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
                VSpacer(height = 12.dp)
                subhead1_leah(text = "Quote expires in ${uiState.expiresIn}")
            }
        }
    ) {
        SectionUniversalLawrence {
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
        }

        uiState.totalFee?.let { totalFee ->
            VSpacer(height = 16.dp)
            SectionUniversalLawrence {
                SwapInfoRow(
                    borderTop = true,
                    title = stringResource(id = R.string.Fee_Total),
                    value = totalFee.getFormattedFull(),
                )
            }
        }

        if (uiState.mevProtectionAvailable) {
            VSpacer(16.dp)

            PremiumHeader()

            SectionPremiumUniversalLawrence {
                CellUniversal {
                    Icon(
                        modifier = Modifier.Companion.size(24.dp),
                        painter = painterResource(id = R.drawable.ic_shield_24),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.jacob
                    )
                    HSpacer(width = 16.dp)
                    body_leah(text = stringResource(R.string.Mev_Protection))
                    HFillSpacer(minWidth = 8.dp)
                    HsSwitch(
                        checked = uiState.mevProtectionEnabled,
                        onCheckedChange = {
                            navController.paidAction(LossProtection) {
                                viewModel.toggleMevProtection(it)
                            }
                        }
                    )
                }
            }
        }

        if (uiState.cautions.isNotEmpty()) {
            Cautions(cautions = uiState.cautions)
        }
    }
}