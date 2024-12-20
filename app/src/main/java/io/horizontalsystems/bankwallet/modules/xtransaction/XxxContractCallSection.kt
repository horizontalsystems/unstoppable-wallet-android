package io.horizontalsystems.bankwallet.modules.xtransaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun XxxContractCallSection(
    navController: NavController,
    operation: String,
    address: String,
    transactionValue: TransactionValue,
    transactionInfoHelper: TransactionInfoHelper,
) {
    SectionUniversalLawrence {
        XxxSectionHeaderCell(
            title = stringResource(R.string.Transactions_ContractCall),
            value = operation,
            painter = TransactionViewItem.Icon.Platform(BlockchainType.Ton).iconRes?.let {
                painterResource(it)
            }
        )
        val contact = transactionInfoHelper.getContact(address, BlockchainType.Ton)
        XxxAddressCell(
            title = stringResource(R.string.TransactionInfo_To),
            value = address,
            showAddContactButton = contact == null,
            blockchainType = BlockchainType.Ton,
            statPage = StatPage.TonConnect,
            statSection = StatSection.AddressTo,
            navController = navController
        )

        XxxAmountCellTV(
            title = stringResource(R.string.Send_Confirmation_YouSend),
            transactionValue = transactionValue,
            coinAmountColor = AmountColor.Negative,
            coinAmountSign = AmountSign.Minus,
            transactionInfoHelper = transactionInfoHelper,
            navController = navController,
            statPage = StatPage.TonConnect
        )
    }
}