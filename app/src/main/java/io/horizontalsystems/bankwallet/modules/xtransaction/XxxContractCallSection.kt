package io.horizontalsystems.bankwallet.modules.xtransaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
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
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
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
        XxxAddress(
            title = stringResource(R.string.TransactionInfo_To),
            value = address,
            showAdd = contact == null,
            blockchainType = BlockchainType.Ton,
            navController = navController,
            onCopy = {
                stat(
                    page = StatPage.TonConnect,
                    section = StatSection.AddressTo,
                    event = StatEvent.Copy(StatEntity.Address)
                )
            },
            onAddToExisting = {
                stat(
                    page = StatPage.TonConnect,
                    section = StatSection.AddressTo,
                    event = StatEvent.Open(StatPage.ContactAddToExisting)
                )
            },
            onAddToNew = {
                stat(
                    page = StatPage.TonConnect,
                    section = StatSection.AddressTo,
                    event = StatEvent.Open(StatPage.ContactNew)
                )
            }
        )

        val xRate = transactionInfoHelper.getXRate(transactionValue.coinUid)

        XxxAmountCell(
            title = stringResource(R.string.Send_Confirmation_YouSend),
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
                sign = "-"
            ),
            coinAmountColor = ComposeAppTheme.colors.lucian,
            fiatAmount = xxxFiatAmount(
                value = xRate?.let {
                    transactionValue.decimalValue?.abs()?.multiply(it)
                },
                fiatSymbol = transactionInfoHelper.getCurrencySymbol()
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
            }
        )
    }
}