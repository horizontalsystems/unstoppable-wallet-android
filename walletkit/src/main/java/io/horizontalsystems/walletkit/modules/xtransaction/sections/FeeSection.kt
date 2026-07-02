package io.horizontalsystems.walletkit.modules.xtransaction.sections

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.entities.CurrencyValue
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.modules.amount.AmountInputType
import io.horizontalsystems.walletkit.modules.fee.HSFee
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.xtransaction.helpers.TransactionInfoHelper

@Composable
fun FeeSection(
    transactionInfoHelper: TransactionInfoHelper,
    fee: TransactionValue.CoinValue,
    navigation: HSNavigation,
) {
    val rateCurrencyValue = transactionInfoHelper.getXRate(fee.coinUid)?.let {
        CurrencyValue(
            currency = transactionInfoHelper.getCurrency(),
            value = it
        )
    }
    HSFee(
        coinCode = fee.coinCode,
        coinDecimal = fee.decimals,
        fee = fee.value,
        amountInputType = AmountInputType.COIN,
        rate = rateCurrencyValue,
        navigation = navigation
    )
}