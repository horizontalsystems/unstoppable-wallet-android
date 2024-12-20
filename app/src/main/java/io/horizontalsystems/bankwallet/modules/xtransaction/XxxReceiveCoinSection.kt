package io.horizontalsystems.bankwallet.modules.xtransaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun XxxReceiveCoinSection(
    transactionValue: TransactionValue,
    address: String,
    comment: String?,
    statPage: StatPage,
    navController: NavController,
    transactionInfoHelper: TransactionInfoHelper,
    blockchainType: BlockchainType,
) {
    XxxTransferCoinSection(
        amountTitle = stringResource(R.string.Send_Confirmation_YouReceive),
        transactionValue = transactionValue,
        coinAmountColor = AmountColor.Positive,
        coinAmountSign = AmountSign.Plus,
        addressTitle = stringResource(R.string.TransactionInfo_From),
        address = address,
        comment = comment,
        statPage = statPage,
        addressStatSection = StatSection.AddressFrom,
        navController = navController,
        transactionInfoHelper = transactionInfoHelper,
        blockchainType = blockchainType,
    )
}