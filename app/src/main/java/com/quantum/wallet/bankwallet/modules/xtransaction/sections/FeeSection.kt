package com.quantum.wallet.bankwallet.modules.xtransaction.sections

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.entities.CurrencyValue
import com.quantum.wallet.bankwallet.entities.TransactionValue
import com.quantum.wallet.bankwallet.modules.amount.AmountInputType
import com.quantum.wallet.bankwallet.modules.fee.HSFee
import com.quantum.wallet.bankwallet.modules.xtransaction.helpers.TransactionInfoHelper

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