package io.horizontalsystems.bankwallet.modules.transactions.q

import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionRecordDataSource
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.Observable
import java.math.BigDecimal
import java.util.*

class TransactionRecordRepository(private val dataSource: TransactionRecordDataSource) {
    val itemsObservable: Observable<List<TransactionRecord>> = Observable.just(
        listOf(
            BitcoinIncomingTransactionRecord(
                Coin(CoinType.Bitcoin, "BTC", "Super", 8),
                UUID.randomUUID().toString(),
                "super",
                0,
                null,
                null,
                Date().time / 1000,
                null,
                false,
                null,
                null,
                false,
                BigDecimal.TEN,
                "its daddy",
                null
            )
        )
    )

    init {

    }
}