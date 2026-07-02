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