package io.horizontalsystems.core.modules.xtransaction.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.core.R
import io.horizontalsystems.core.core.stats.StatPage
import io.horizontalsystems.core.core.stats.StatSection
import io.horizontalsystems.core.entities.TransactionValue
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.xtransaction.cells.AddressCell
import io.horizontalsystems.core.modules.xtransaction.cells.AmountCellTV
import io.horizontalsystems.core.modules.xtransaction.cells.AmountColor
import io.horizontalsystems.core.modules.xtransaction.cells.AmountSign
import io.horizontalsystems.core.modules.xtransaction.cells.TitleAndValueCell
import io.horizontalsystems.core.modules.xtransaction.helpers.TransactionInfoHelper
import io.horizontalsystems.core.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun TransferCoinSection(
    amountTitle: String,
    transactionValue: TransactionValue,
    coinAmountColor: AmountColor,
    coinAmountSign: AmountSign,
    addressTitle: String,
    address: String,
    comment: String?,
    statPage: StatPage,
    addressStatSection: StatSection,
    navigation: HSNavigation,
    transactionInfoHelper: TransactionInfoHelper,
    blockchainType: BlockchainType,
) {
    SectionUniversalLawrence {
        AmountCellTV(
            title = amountTitle,
            transactionValue = transactionValue,
            coinAmountColor = coinAmountColor,
            coinAmountSign = coinAmountSign,
            transactionInfoHelper = transactionInfoHelper,
            navigation = navigation,
            statPage = statPage,
            borderTop = false
        )

        val contact = transactionInfoHelper.getContact(address, blockchainType)

        AddressCell(
            title = addressTitle,
            value = address,
            showAddContactButton = contact == null,
            blockchainType = blockchainType,
            statPage = statPage,
            statSection = addressStatSection,
            navigation = navigation
        )
        contact?.let {
            TitleAndValueCell(
                title = stringResource(R.string.TransactionInfo_ContactName),
                value = it.name
            )
        }
        comment?.let {
            TitleAndValueCell(
                title = stringResource(R.string.TransactionInfo_Memo),
                value = it
            )
        }
    }
}