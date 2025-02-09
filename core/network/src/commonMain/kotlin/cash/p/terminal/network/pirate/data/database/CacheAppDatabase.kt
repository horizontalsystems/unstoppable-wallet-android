package cash.p.terminal.network.pirate.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cash.p.terminal.network.pirate.data.database.converter.CoinAssociationListConverter
import cash.p.terminal.network.pirate.data.database.entity.ChangeNowAssociationCoin

@Database(entities = [ChangeNowAssociationCoin::class], version = 1, exportSchema = false)
@TypeConverters(CoinAssociationListConverter::class)
internal abstract class CacheAppDatabase : RoomDatabase() {
    abstract fun changeNowCoinDao(): CacheChangeNowCoinAssociationDao

    companion object {
        fun buildDatabase(context: Context): CacheAppDatabase {
            return Room.databaseBuilder(context, CacheAppDatabase::class.java, "db_cache")
                .build()
        }
    }
}