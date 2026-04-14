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
fun MintSection(
    transactionValue: TransactionValue,
    transactionInfoHelper: TransactionInfoHelper,
    navController: NavController,
) {
    SectionUniversalLawrence {
        AmountCellTV(
            title = stringResource(R.string.Send_Confirmation_Mint),
            transactionValue = transactionValue,
            coinAmountColor = AmountColor.Positive,
            coinAmountSign = AmountSign.Plus,
            transactionInfoHelper = transactionInfoHelper,
            navController = navController,
            statPage = StatPage.TonConnect,
            borderTop = false,
        )
    }
}