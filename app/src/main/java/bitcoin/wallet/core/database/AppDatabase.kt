package bitcoin.wallet.core.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import bitcoin.wallet.entities.ExchangeRate
import bitcoin.wallet.entities.ExchangeRateDao
import bitcoin.wallet.entities.UnspentOutput
import bitcoin.wallet.entities.UnspentOutputDao

@Database(entities = [UnspentOutput::class, ExchangeRate::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun unspentOutputDao(): UnspentOutputDao

    abstract fun exchangeRateDao(): ExchangeRateDao

}
