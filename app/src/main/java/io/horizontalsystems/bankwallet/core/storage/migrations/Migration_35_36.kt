package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_35_36 : Migration(35, 36) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `CustomToken` (`coinName` TEXT NOT NULL, `coinCode` TEXT NOT NULL, `coinType` TEXT NOT NULL, `decimal` INTEGER NOT NULL, PRIMARY KEY(`coinType`))")

        db.execSQL("ALTER TABLE FavoriteCoin RENAME TO TempFavoriteCoin")
        db.execSQL("CREATE TABLE IF NOT EXISTS `FavoriteCoin` (`coinUid` TEXT NOT NULL, PRIMARY KEY(`coinUid`))")
        db.execSQL("INSERT INTO FavoriteCoin (`coinUid`) SELECT `coinType` FROM TempFavoriteCoin")

        db.execSQL("DROP TABLE TempFavoriteCoin")
        db.execSQL("DROP TABLE SubscriptionJob")
        db.execSQL("DROP TABLE PriceAlert")
    }
}
