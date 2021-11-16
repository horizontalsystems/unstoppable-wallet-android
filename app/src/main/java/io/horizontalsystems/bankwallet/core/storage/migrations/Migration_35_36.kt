package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_35_36 : Migration(35, 36) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `CustomToken` (`coinName` TEXT NOT NULL, `coinCode` TEXT NOT NULL, `coinType` TEXT NOT NULL, `decimal` INTEGER NOT NULL, PRIMARY KEY(`coinType`))")

        database.execSQL("ALTER TABLE FavoriteCoin RENAME TO TempFavoriteCoin")
        database.execSQL("CREATE TABLE IF NOT EXISTS `FavoriteCoin` (`coinUid` TEXT NOT NULL, PRIMARY KEY(`coinUid`))")
        database.execSQL("INSERT INTO FavoriteCoin (`coinUid`) SELECT `coinType` FROM TempFavoriteCoin")

        database.execSQL("DROP TABLE TempFavoriteCoin")
        database.execSQL("DROP TABLE SubscriptionJob")
        database.execSQL("DROP TABLE PriceAlert")
    }
}
