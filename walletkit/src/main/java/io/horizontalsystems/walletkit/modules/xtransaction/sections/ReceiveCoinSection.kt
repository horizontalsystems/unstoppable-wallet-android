package io.horizontalsystems.walletkit.modules.xtransaction.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.StatSection
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.xtransaction.cells.AmountColor
import io.horizontalsystems.walletkit.modules.xtransaction.cells.AmountSign
import io.horizontalsystems.walletkit.modules.xtransaction.helpers.TransactionInfoHelper
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