package io.horizontalsystems.bankwallet.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters
import io.horizontalsystems.bankwallet.core.storage.TransactionAddressTypeConverter
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

@Entity
open class TransactionRecord {

    @PrimaryKey
    var transactionHash = ""
    var blockHeight: Long = 0
    var coinCode = ""
    var amount: Double = 0.0
    var timestamp: Long = 0
    var rate: Double = 0.0

    @TypeConverters(TransactionAddressTypeConverter::class)
    var from: List<TransactionAddress> = listOf()

    @TypeConverters(TransactionAddressTypeConverter::class)
    var to: List<TransactionAddress> = listOf()

}

data class TransactionItem(val coinCode: CoinCode, val record: TransactionRecord)

class TransactionAddress {
    var address = ""
    var mine = false
}
