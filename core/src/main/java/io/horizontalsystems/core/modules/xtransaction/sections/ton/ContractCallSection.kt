package io.horizontalsystems.core.modules.xtransaction.sections.ton

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.core.R
import io.horizontalsystems.core.core.stats.StatPage
import io.horizontalsystems.core.core.stats.StatSection
import io.horizontalsystems.core.entities.TransactionValue
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.transactions.TransactionViewItem
import io.horizontalsystems.core.modules.xtransaction.cells.AddressCell
import io.horizontalsystems.core.modules.xtransaction.cells.AmountCellTV
import io.horizontalsystems.core.modules.xtransaction.cells.AmountColor
import io.horizontalsystems.core.modules.xtransaction.cells.AmountSign
import io.horizontalsystems.core.modules.xtransaction.cells.HeaderCell
import io.horizontalsystems.core.modules.xtransaction.helpers.TransactionInfoHelper
import io.horizontalsystems.core.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun ContractCallSection(
    navigation: HSNavigation,
    operation: String,
    address: String,
    transactionValue: TransactionValue,
    transactionInfoHelper: TransactionInfoHelper,
    blockchainType: BlockchainType,
) {
    SectionUniversalLawrence {
        HeaderCell(
            title = stringResource(R.string.Transactions_ContractCall),
            value = operation,
            painter = TransactionViewItem.Icon.Platform(blockchainType).iconRes?.let {
                painterResource(it)
            }
        )
        val contact = transactionInfoHelper.getContact(address, blockchainType)
        AddressCell(
            title = stringResource(R.string.TransactionInfo_To),
            value = address,
            showAddContactButton = contact == null,
            blockchainType = blockchainType,
            statPage = StatPage.TonConnect,
            statSection = StatSection.AddressTo,
            navigation = navigation
        )

        AmountCellTV(
            title = stringResource(R.string.Send_Confirmation_YouSend),
            transactionValue = transactionValue,
            coinAmountColor = AmountColor.Negative,
            coinAmountSign = AmountSign.Minus,
            transactionInfoHelper = transactionInfoHelper,
            navigation = navigation,
            statPage = StatPage.TonConnect
        )
    }
}