package io.horizontalsystems.bankwallet.modules.xtransaction.sections

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.fee.HSFee
import io.horizontalsystems.bankwallet.modules.xtransaction.helpers.TransactionInfoHelper

@Composable
fun FeeSection(
    transactionInfoHelper: TransactionInfoHelper,
    fee: TransactionValue.CoinValue,
    navController: NavController,
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
        navController = navController
    )
}