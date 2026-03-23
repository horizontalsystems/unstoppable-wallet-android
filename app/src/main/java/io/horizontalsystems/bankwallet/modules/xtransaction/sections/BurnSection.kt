package io.horizontalsystems.bankwallet.modules.xtransaction.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.xtransaction.cells.AmountCellTV
import io.horizontalsystems.bankwallet.modules.xtransaction.cells.AmountColor
import io.horizontalsystems.bankwallet.modules.xtransaction.cells.AmountSign
import io.horizontalsystems.bankwallet.modules.xtransaction.helpers.TransactionInfoHelper
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence

@Composable
fun BurnSection(
    transactionValue: TransactionValue,
    transactionInfoHelper: TransactionInfoHelper,
    navController: NavBackStack<HSScreen>,
) {
    SectionUniversalLawrence {
        AmountCellTV(
            title = stringResource(R.string.Send_Confirmation_Burn),
            transactionValue = transactionValue,
            coinAmountColor = AmountColor.Negative,
            coinAmountSign = AmountSign.Minus,
            transactionInfoHelper = transactionInfoHelper,
            navController = navController,
            statPage = StatPage.TonConnect,
            borderTop = false,
        )
    }
}
