package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_47_48 : Migration(47, 48) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `NftCollectionRecord_new` (`blockchainType` TEXT NOT NULL, `accountId` TEXT NOT NULL, `uid` TEXT NOT NULL, `name` TEXT NOT NULL, `imageUrl` TEXT, `averagePrice7d_tokenQueryId` TEXT, `averagePrice7d_value` TEXT, `averagePrice30d_tokenQueryId` TEXT, `averagePrice30d_value` TEXT, PRIMARY KEY(`blockchainType`, `accountId`, `uid`), FOREIGN KEY(`accountId`) REFERENCES `AccountRecord`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")
        db.execSQL("DROP TABLE NftCollectionRecord" )
        db.execSQL("ALTER TABLE NftCollectionRecord_new RENAME TO NftCollectionRecord")

        db.execSQL("CREATE TABLE IF NOT EXISTS `NftAssetRecord_new` (`blockchainType` TEXT NOT NULL, `accountId` TEXT NOT NULL, `nftUid` TEXT NOT NULL, `collectionUid` TEXT NOT NULL, `name` TEXT, `imagePreviewUrl` TEXT, `onSale` INTEGER NOT NULL, `lastSale_tokenQueryId` TEXT, `lastSale_value` TEXT, PRIMARY KEY(`blockchainType`, `accountId`, `nftUid`), FOREIGN KEY(`accountId`) REFERENCES `AccountRecord`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")
        db.execSQL("DROP TABLE NftAssetRecord" )
        db.execSQL("ALTER TABLE NftAssetRecord_new RENAME TO NftAssetRecord")

        db.execSQL("CREATE TABLE IF NOT EXISTS `NftMetadataSyncRecord` (`blockchainType` TEXT NOT NULL, `accountId` TEXT NOT NULL, `lastSyncTimestamp` INTEGER NOT NULL, PRIMARY KEY(`blockchainType`, `accountId`))")
    }
}

