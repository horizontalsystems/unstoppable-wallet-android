package io.horizontalsystems.bankwallet.modules.xtransaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun XxxSendReceiveSection(
    amountTitle: String,
    transactionValue: TransactionValue,
    coinAmountColor: AmountColor,
    coinAmountSign: AmountSign,
    addressTitle: String,
    address: String,
    comment: String?,
    statPage: StatPage,
    addressStatSection: StatSection,
    navController: NavController,
    transactionInfoHelper: TransactionInfoHelper,
    blockchainType: BlockchainType,
) {
    SectionUniversalLawrence {
        XxxAmountCellTV(
            title = amountTitle,
            transactionValue = transactionValue,
            coinAmountColor = coinAmountColor,
            coinAmountSign = coinAmountSign,
            transactionInfoHelper = transactionInfoHelper,
            navController = navController,
            statPage = statPage,
            borderTop = false
        )

        val contact = transactionInfoHelper.getContact(address, blockchainType)

        XxxAddress(
            title = addressTitle,
            value = address,
            showAdd = contact == null,
            blockchainType = blockchainType,
            statPage = statPage,
            statSection = addressStatSection,
            navController = navController
        )
        contact?.let {
            XxxTitleAndValueCell(
                title = stringResource(R.string.TransactionInfo_ContactName),
                value = it.name
            )
        }
        comment?.let {
            XxxTitleAndValueCell(
                title = stringResource(R.string.TransactionInfo_Memo),
                value = it
            )
        }
    }
}