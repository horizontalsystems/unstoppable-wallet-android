package cash.p.terminal.modules.multiswap.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.modules.evmfee.FeeSettingsInfoDialog
import cash.p.terminal.modules.multiswap.QuoteInfoRow
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_lucian
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

data class DataFieldAllowance(val allowance: BigDecimal, val token: Token) : DataField {
    @Composable
    override fun GetContent(navController: NavController, borderTop: Boolean) {
        val infoTitle = stringResource(id = R.string.SwapInfo_AllowanceTitle)
        val infoText = stringResource(id = R.string.SwapInfo_AllowanceDescription)

        QuoteInfoRow(
            borderTop = borderTop,
            title = {
                subhead2_grey(text = stringResource(R.string.Swap_Allowance))

                Image(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable(
                            onClick = {
                                navController.slideFromBottom(
                                    R.id.feeSettingsInfoDialog,
                                    FeeSettingsInfoDialog.Input(infoTitle, infoText)
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
                subhead2_lucian(text = CoinValue(token, allowance).getFormattedFull())
            }
        )
    }
}
