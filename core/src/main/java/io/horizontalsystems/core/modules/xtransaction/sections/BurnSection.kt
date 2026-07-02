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
fun BurnSection(
    transactionValue: TransactionValue,
    transactionInfoHelper: TransactionInfoHelper,
    navigation: HSNavigation,
) {
    SectionUniversalLawrence {
        AmountCellTV(
            title = stringResource(R.string.Send_Confirmation_Burn),
            transactionValue = transactionValue,
            coinAmountColor = AmountColor.Negative,
            coinAmountSign = AmountSign.Minus,
            transactionInfoHelper = transactionInfoHelper,
            navigation = navigation,
            statPage = StatPage.TonConnect,
            borderTop = false,
        )
    }
}
