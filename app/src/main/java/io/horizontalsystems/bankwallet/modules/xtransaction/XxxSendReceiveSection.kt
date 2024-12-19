package io.horizontalsystems.bankwallet.modules.xtransaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.marketkit.models.BlockchainType

@Composable
fun XxxSendReceiveSection(
    transactionValue: TransactionValue,
    amountTitle: String,
    sign: String,
    coinAmountColor: Color,
    navController: NavController,
    address: String,
    comment: String?,
    addressTitle: String,
    addressStatSection: StatSection,
) {
    val helper = remember {
        SendReceiveHelper()
    }

    SectionUniversalLawrence {
        val xRate = helper.getXRate(transactionValue.coinUid)

        XxxAmount(
            title = amountTitle,
            coinIcon = coinIconPainter(
                url = transactionValue.coinIconUrl,
                alternativeUrl = transactionValue.alternativeCoinIconUrl,
                placeholder = transactionValue.coinIconPlaceholder
            ),
            coinProtocolType = transactionValue.badge
                ?: stringResource(id = R.string.CoinPlatforms_Native),
            coinAmount = xxxCoinAmount(
                value = transactionValue.decimalValue?.abs(),
                coinCode = transactionValue.coinCode,
                sign = sign
            ),
            coinAmountColor = coinAmountColor,
            fiatAmount = xxxFiatAmount(
                value = xRate?.let {
                    transactionValue.decimalValue?.abs()?.multiply(it)
                },
                fiatSymbol = helper.getCurrencySymbol()
            ),
            onClick = {
                navController.slideFromRight(
                    R.id.coinFragment,
                    CoinFragment.Input(transactionValue.coinUid)
                )

                stat(
                    page = StatPage.TonConnect,
                    event = StatEvent.OpenCoin(transactionValue.coinUid)
                )
            },
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
            XxxContact(name = it.name)
        }
        comment?.let {
            XxxTitleAndValueCell(
                title = stringResource(R.string.TransactionInfo_Memo),
                value = it
            )
        }
    }
}