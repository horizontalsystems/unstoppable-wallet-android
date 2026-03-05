package io.horizontalsystems.bankwallet.modules.xtransaction.sections

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.xtransaction.helpers.TransactionInfoHelper

@Composable
fun FeeSection(
    transactionInfoHelper: TransactionInfoHelper,
    fee: TransactionValue.CoinValue,
    backStack: NavBackStack<HSScreen>,
) {
    val rateCurrencyValue = transactionInfoHelper.getXRate(fee.coinUid)?.let {
        CurrencyValue(
            currency = transactionInfoHelper.getCurrency(),
            value = it
        )
    }
//    TODO("xxx nav3")
//    HSFee(
//        coinCode = fee.coinCode,
//        coinDecimal = fee.decimals,
//        fee = fee.value,
//        amountInputType = AmountInputType.COIN,
//        rate = rateCurrencyValue,
//        navController = backStack
//    )
}