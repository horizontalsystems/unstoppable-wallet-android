package io.horizontalsystems.bankwallet.modules.xtransaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence

@Composable
fun XxxSwapSection(
    transactionInfoHelper: TransactionInfoHelper,
    navController: NavController,
    transactionValueIn: TransactionValue,
    transactionValueOut: TransactionValue,
) {
    SectionUniversalLawrence {
        XxxAmountCellTV(
            title = stringResource(R.string.Send_Confirmation_YouSend),
            transactionValue = transactionValueIn,
            coinAmountColor = AmountColor.Negative,
            coinAmountSign = AmountSign.Minus,
            transactionInfoHelper = transactionInfoHelper,
            navController = navController,
            statPage = StatPage.TonConnect,
            borderTop = false,
        )

        XxxAmountCellTV(
            title = stringResource(R.string.Swap_YouGet),
            transactionValue = transactionValueOut,
            coinAmountColor = AmountColor.Positive,
            coinAmountSign = AmountSign.Plus,
            transactionInfoHelper = transactionInfoHelper,
            navController = navController,
            statPage = StatPage.TonConnect,
            borderTop = true,
        )
    }
}