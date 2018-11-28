package io.horizontalsystems.bankwallet.core.storage

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.TransactionRecord

@Database(entities = [TransactionRecord::class, Rate::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun ratesDao(): RatesDao

}
