package io.horizontalsystems.bankwallet.modules.xtransaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence

@Composable
fun XxxBurnSection(
    transactionValue: TransactionValue,
    transactionInfoHelper: TransactionInfoHelper,
    navController: NavController,
) {
    SectionUniversalLawrence {
        XxxAmount(
            title = stringResource(R.string.Send_Confirmation_Burn),
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
                value = transactionInfoHelper.getXRate(transactionValue.coinUid)
                    ?.let {
                        transactionValue.decimalValue?.abs()
                            ?.multiply(it)
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
            },
            borderTop = false
        )
    }
}