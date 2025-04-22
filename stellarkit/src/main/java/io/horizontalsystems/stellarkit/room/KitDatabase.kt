package io.horizontalsystems.stellarkit.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AssetBalance::class,
        AssetNativeBalance::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class KitDatabase : RoomDatabase() {
    abstract fun balanceDao(): BalanceDao

    companion object {
        fun getInstance(context: Context, name: String): KitDatabase {
            return Room.databaseBuilder(context, KitDatabase::class.java, name)
                .build()
        }
    }
}
