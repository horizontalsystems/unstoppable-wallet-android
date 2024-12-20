package io.horizontalsystems.bankwallet.modules.xtransaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun XxxSendReceiveSection(
    transactionValue: TransactionValue,
    amountTitle: String,
    coinAmountColor: AmountColor,
    coinAmountSign: AmountSign,
    navController: NavController,
    address: String,
    comment: String?,
    addressTitle: String,
    addressStatSection: StatSection,
    helper: TransactionInfoHelper,
) {
    SectionUniversalLawrence {
        XxxAmountCellTV(
            title = amountTitle,
            transactionValue = transactionValue,
            coinAmountColor = coinAmountColor,
            coinAmountSign = coinAmountSign,
            transactionInfoHelper = helper,
            navController = navController,
            statPage = StatPage.TonConnect,
            borderTop = false
        )

        val contact = helper.getContact(address, BlockchainType.Ton)

        XxxAddress(
            title = addressTitle,
            value = address,
            showAdd = contact == null,
            blockchainType = BlockchainType.Ton,
            navController = navController,
            onCopy = {
                stat(
                    page = StatPage.TonConnect,
                    section = addressStatSection,
                    event = StatEvent.Copy(StatEntity.Address)
                )
            },
            onAddToExisting = {
                stat(
                    page = StatPage.TonConnect,
                    section = addressStatSection,
                    event = StatEvent.Open(StatPage.ContactAddToExisting)
                )
            },
            onAddToNew = {
                stat(
                    page = StatPage.TonConnect,
                    section = addressStatSection,
                    event = StatEvent.Open(StatPage.ContactNew)
                )
            }
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