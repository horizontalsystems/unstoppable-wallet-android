package io.horizontalsystems.core.modules.xtransaction.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.core.R
import io.horizontalsystems.core.core.stats.StatPage
import io.horizontalsystems.core.entities.TransactionValue
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.xtransaction.cells.AmountCellTV
import io.horizontalsystems.core.modules.xtransaction.cells.AmountColor
import io.horizontalsystems.core.modules.xtransaction.cells.AmountSign
import io.horizontalsystems.core.modules.xtransaction.helpers.TransactionInfoHelper
import io.horizontalsystems.core.ui.compose.components.cell.SectionUniversalLawrence

@Composable
fun SwapSection(
    transactionInfoHelper: TransactionInfoHelper,
    navigation: HSNavigation,
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
            navigation = navigation,
            statPage = StatPage.TonConnect,
            borderTop = false,
        )

        AmountCellTV(
            title = stringResource(R.string.Swap_YouGet),
            transactionValue = transactionValueOut,
            coinAmountColor = AmountColor.Positive,
            coinAmountSign = AmountSign.Plus,
            transactionInfoHelper = transactionInfoHelper,
            navigation = navigation,
            statPage = StatPage.TonConnect,
            borderTop = true,
        )
    }
}