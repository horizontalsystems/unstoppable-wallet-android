package io.horizontalsystems.bankwallet.modules.xtransaction.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.xtransaction.cells.AmountCellTV
import io.horizontalsystems.bankwallet.modules.xtransaction.cells.AmountColor
import io.horizontalsystems.bankwallet.modules.xtransaction.cells.AmountSign
import io.horizontalsystems.bankwallet.modules.xtransaction.helpers.TransactionInfoHelper
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence

@Composable
fun SwapSection(
    transactionInfoHelper: TransactionInfoHelper,
    navController: NavController,
    transactionValueIn: TransactionValue,
    transactionValueOut: TransactionValue,
) {
    SectionUniversalLawrence {
        AmountCellTV(
            title = stringResource(R.string.Send_Confirmation_YouSend),
            transactionValue = transactionValueIn,
            coinAmountColor = AmountColor.Negative,
            coinAmountSign = AmountSign.Minus,
            transactionInfoHelper = transactionInfoHelper,
            navController = navController,
            statPage = StatPage.TonConnect,
            borderTop = false,
        )

        AmountCellTV(
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