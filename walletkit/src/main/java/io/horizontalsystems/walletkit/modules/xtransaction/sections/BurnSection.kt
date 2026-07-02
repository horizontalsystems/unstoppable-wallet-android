package io.horizontalsystems.walletkit.modules.xtransaction.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.xtransaction.cells.AmountCellTV
import io.horizontalsystems.walletkit.modules.xtransaction.cells.AmountColor
import io.horizontalsystems.walletkit.modules.xtransaction.cells.AmountSign
import io.horizontalsystems.walletkit.modules.xtransaction.helpers.TransactionInfoHelper
import io.horizontalsystems.walletkit.ui.compose.components.cell.SectionUniversalLawrence

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
