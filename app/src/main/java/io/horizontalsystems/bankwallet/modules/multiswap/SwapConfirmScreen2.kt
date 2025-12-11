package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.paidAction
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.FeeType
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFee2
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFeeTemplate
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.PremiumHeader
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionPremiumUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.subscriptions.core.LossProtection

@Composable
fun SwapConfirmScreen2(
    navController: NavController,
    uiState: SwapUiState,
    viewModel: SwapViewModel
) {
    BottomSheetHeaderV3(
        title = stringResource(R.string.SwapConfirm_Title)
    )

    Column(
        modifier = Modifier
            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
            .border(1.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp)
    ) {
        PriceImpactField(uiState.priceImpact, uiState.priceImpactLevel, navController)
        uiState.amountOutMin?.let { amountOutMin ->
//                val subvalue = uiState.fiatAmountOutMin?.let { fiatAmountOutMin ->
//                    CurrencyValue(uiState.currency, fiatAmountOutMin).getFormattedFull()
//                } ?: "---"
            val subvalue = null

            SwapInfoRow2(
                title = stringResource(id = R.string.Swap_MinimumReceived).hs,
                value = CoinValue(uiState.tokenOut!!, amountOutMin).getFormattedFull().hs,
                subvalue = subvalue
            )
        }
        uiState.quoteFields.forEach {
            it.GetContent(navController, true)
        }
        DataFieldFee2(
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
                borderTop = true,
                title = stringResource(id = R.string.Fee_Total),
                value = totalFee.getFormattedFull(),
            )
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
