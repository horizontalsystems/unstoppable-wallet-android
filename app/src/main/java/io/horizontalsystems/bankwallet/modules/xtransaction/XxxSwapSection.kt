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
fun XxxSwapSection(
    transactionInfoHelper: TransactionInfoHelper,
    navController: NavController,
    transactionValueIn: TransactionValue,
    transactionValueOut: TransactionValue,
) {
    SectionUniversalLawrence {
        val xRateIn = transactionInfoHelper.getXRate(transactionValueIn.coinUid)

        XxxAmount(
            title = stringResource(R.string.Send_Confirmation_YouSend),
            coinIcon = coinIconPainter(
                url = transactionValueIn.coinIconUrl,
                alternativeUrl = transactionValueIn.alternativeCoinIconUrl,
                placeholder = transactionValueIn.coinIconPlaceholder
            ),
            coinProtocolType = transactionValueIn.badge
                ?: stringResource(id = R.string.CoinPlatforms_Native),
            coinAmount = xxxCoinAmount(
                value = transactionValueIn.decimalValue?.abs(),
                coinCode = transactionValueIn.coinCode,
                sign = "-"
            ),
            coinAmountColor = ComposeAppTheme.colors.lucian,
            fiatAmount = xxxFiatAmount(
                value = xRateIn?.let {
                    transactionValueIn.decimalValue?.abs()?.multiply(it)
                },
                fiatSymbol = transactionInfoHelper.getCurrencySymbol()
            ),
            onClick = {
                navController.slideFromRight(
                    R.id.coinFragment,
                    CoinFragment.Input(transactionValueIn.coinUid)
                )

                stat(
                    page = StatPage.TonConnect,
                    event = StatEvent.OpenCoin(transactionValueIn.coinUid)
                )
            }
        )

        val xRateOut = transactionInfoHelper.getXRate(transactionValueOut.coinUid)

        XxxAmount(
            title = stringResource(R.string.Swap_YouGet),
            coinIcon = coinIconPainter(
                url = transactionValueOut.coinIconUrl,
                alternativeUrl = transactionValueOut.alternativeCoinIconUrl,
                placeholder = transactionValueOut.coinIconPlaceholder
            ),
            coinProtocolType = transactionValueOut.badge
                ?: stringResource(id = R.string.CoinPlatforms_Native),
            coinAmount = xxxCoinAmount(
                value = transactionValueOut.decimalValue?.abs(),
                coinCode = transactionValueOut.coinCode,
                sign = "-"
            ),
            coinAmountColor = ComposeAppTheme.colors.remus,
            fiatAmount = xxxFiatAmount(
                value = xRateOut?.let {
                    transactionValueOut.decimalValue?.abs()?.multiply(it)
                },
                fiatSymbol = transactionInfoHelper.getCurrencySymbol()
            ),
            onClick = {
                navController.slideFromRight(
                    R.id.coinFragment,
                    CoinFragment.Input(transactionValueOut.coinUid)
                )

                stat(
                    page = StatPage.TonConnect,
                    event = StatEvent.OpenCoin(transactionValueOut.coinUid)
                )
            }
        )
    }
}