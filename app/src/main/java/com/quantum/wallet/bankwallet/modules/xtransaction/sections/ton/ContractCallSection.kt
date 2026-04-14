package com.quantum.wallet.bankwallet.modules.xtransaction.sections.ton

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.StatSection
import com.quantum.wallet.bankwallet.entities.TransactionValue
import com.quantum.wallet.bankwallet.modules.transactions.TransactionViewItem
import com.quantum.wallet.bankwallet.modules.xtransaction.cells.AddressCell
import com.quantum.wallet.bankwallet.modules.xtransaction.cells.AmountCellTV
import com.quantum.wallet.bankwallet.modules.xtransaction.cells.AmountColor
import com.quantum.wallet.bankwallet.modules.xtransaction.cells.AmountSign
import com.quantum.wallet.bankwallet.modules.xtransaction.cells.HeaderCell
import com.quantum.wallet.bankwallet.modules.xtransaction.helpers.TransactionInfoHelper
import com.quantum.wallet.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun ContractCallSection(
    navController: NavController,
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
            navController = navController
        )

        AmountCellTV(
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