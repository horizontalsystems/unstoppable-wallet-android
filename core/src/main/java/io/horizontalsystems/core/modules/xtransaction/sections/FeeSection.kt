package io.horizontalsystems.core.modules.xtransaction.sections

import androidx.compose.runtime.Composable
import io.horizontalsystems.core.entities.CurrencyValue
import io.horizontalsystems.core.entities.TransactionValue
import io.horizontalsystems.core.modules.amount.AmountInputType
import io.horizontalsystems.core.modules.fee.HSFee
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.xtransaction.helpers.TransactionInfoHelper

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