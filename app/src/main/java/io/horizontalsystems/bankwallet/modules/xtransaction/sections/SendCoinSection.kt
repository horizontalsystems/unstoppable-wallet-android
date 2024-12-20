package io.horizontalsystems.bankwallet.modules.xtransaction.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.xtransaction.cells.AmountColor
import io.horizontalsystems.bankwallet.modules.xtransaction.cells.AmountSign
import io.horizontalsystems.bankwallet.modules.xtransaction.helpers.TransactionInfoHelper
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun SendCoinSection(
    transactionValue: TransactionValue,
    address: String,
    comment: String?,
    sentToSelf: Boolean,
    statPage: StatPage,
    navController: NavController,
    transactionInfoHelper: TransactionInfoHelper,
    blockchainType: BlockchainType
) {
    TransferCoinSection(
        amountTitle = stringResource(R.string.Send_Confirmation_YouSend),
        transactionValue = transactionValue,
        coinAmountColor = AmountColor.Negative,
        coinAmountSign = if (sentToSelf) AmountSign.None else AmountSign.Minus,
        addressTitle = stringResource(R.string.TransactionInfo_To),
        address = address,
        comment = comment,
        statPage = statPage,
        addressStatSection = StatSection.AddressTo,
        navController = navController,
        transactionInfoHelper = transactionInfoHelper,
        blockchainType = blockchainType,
    )
}