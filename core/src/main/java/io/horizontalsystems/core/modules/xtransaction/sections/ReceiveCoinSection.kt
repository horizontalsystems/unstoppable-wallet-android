package io.horizontalsystems.core.modules.xtransaction.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.core.R
import io.horizontalsystems.core.core.stats.StatPage
import io.horizontalsystems.core.core.stats.StatSection
import io.horizontalsystems.core.entities.TransactionValue
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.xtransaction.cells.AmountColor
import io.horizontalsystems.core.modules.xtransaction.cells.AmountSign
import io.horizontalsystems.core.modules.xtransaction.helpers.TransactionInfoHelper
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun ReceiveCoinSection(
    transactionValue: TransactionValue,
    address: String,
    comment: String?,
    statPage: StatPage,
    navigation: HSNavigation,
    transactionInfoHelper: TransactionInfoHelper,
    blockchainType: BlockchainType,
) {
    TransferCoinSection(
        amountTitle = stringResource(R.string.Send_Confirmation_YouReceive),
        transactionValue = transactionValue,
        coinAmountColor = AmountColor.Positive,
        coinAmountSign = AmountSign.Plus,
        addressTitle = stringResource(R.string.TransactionInfo_From),
        address = address,
        comment = comment,
        statPage = statPage,
        addressStatSection = StatSection.AddressFrom,
        navigation = navigation,
        transactionInfoHelper = transactionInfoHelper,
        blockchainType = blockchainType,
    )
}