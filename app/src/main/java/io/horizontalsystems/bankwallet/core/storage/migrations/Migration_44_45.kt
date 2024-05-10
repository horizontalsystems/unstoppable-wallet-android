package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_44_45 : Migration(44, 45) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `NftCollectionRecord`")
        db.execSQL("CREATE TABLE IF NOT EXISTS `NftCollectionRecord` (`accountId` TEXT NOT NULL, `uid` TEXT NOT NULL, `name` TEXT NOT NULL, `imageUrl` TEXT, `totalSupply` INTEGER NOT NULL, `averagePrice7d_coinTypeId` TEXT, `averagePrice7d_value` TEXT, `averagePrice30d_coinTypeId` TEXT, `averagePrice30d_value` TEXT, `floorPrice_coinTypeId` TEXT, `floorPrice_value` TEXT, `external_url` TEXT, `discord_url` TEXT, `twitter_username` TEXT, PRIMARY KEY(`accountId`, `uid`), FOREIGN KEY(`accountId`) REFERENCES `AccountRecord`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")
    }
}
