package com.quantum.wallet.bankwallet.modules.xtransaction.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.entities.TransactionValue
import com.quantum.wallet.bankwallet.modules.xtransaction.cells.AmountCellTV
import com.quantum.wallet.bankwallet.modules.xtransaction.cells.AmountColor
import com.quantum.wallet.bankwallet.modules.xtransaction.cells.AmountSign
import com.quantum.wallet.bankwallet.modules.xtransaction.helpers.TransactionInfoHelper
import com.quantum.wallet.bankwallet.ui.compose.components.cell.SectionUniversalLawrence

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