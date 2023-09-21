package io.horizontalsystems.marketkit.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


object Migration_8_9: Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS `TokenEntityNew`;")
        database.execSQL("CREATE TABLE IF NOT EXISTS `TokenEntityNew` (`coinUid` TEXT NOT NULL, `blockchainUid` TEXT NOT NULL, `type` TEXT NOT NULL, `decimals` INTEGER, `reference` TEXT NOT NULL, PRIMARY KEY(`coinUid`, `blockchainUid`, `type`, `reference`), FOREIGN KEY(`coinUid`) REFERENCES `Coin`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`blockchainUid`) REFERENCES `BlockchainEntity`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE);")
        database.execSQL("INSERT INTO `TokenEntityNew` SELECT  `coinUid`, `blockchainUid`, `type`, `decimals`, COALESCE(reference, '') FROM `TokenEntity`;")
        database.execSQL("DROP TABLE IF EXISTS `TokenEntity`;")
        database.execSQL("ALTER TABLE `TokenEntityNew` RENAME TO `TokenEntity`;")
        database.execSQL("CREATE INDEX index_TokenEntity_coinUid ON TokenEntity(`coinUid`);")
        database.execSQL("CREATE INDEX index_TokenEntity_blockchainUid ON TokenEntity(`blockchainUid`);")
    }
}
