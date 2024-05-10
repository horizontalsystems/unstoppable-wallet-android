package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_37_38 : Migration(37, 38) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `NftCollectionRecord` (`accountId` TEXT NOT NULL, `uid` TEXT NOT NULL, `name` TEXT NOT NULL, `imageUrl` TEXT, `totalSupply` INTEGER NOT NULL, `averagePrice7d_coinTypeId` TEXT, `averagePrice7d_value` TEXT, `averagePrice30d_coinTypeId` TEXT, `averagePrice30d_value` TEXT, `floorPrice_coinTypeId` TEXT, `floorPrice_value` TEXT, `external_url` TEXT, `discord_url` TEXT, `telegram_url` TEXT, `twitter_username` TEXT, `instagram_username` TEXT, `wiki_url` TEXT, PRIMARY KEY(`accountId`, `uid`), FOREIGN KEY(`accountId`) REFERENCES `AccountRecord`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `NftAssetRecord` (`accountId` TEXT NOT NULL, `collectionUid` TEXT NOT NULL, `tokenId` TEXT NOT NULL, `name` TEXT, `imageUrl` TEXT, `imagePreviewUrl` TEXT, `description` TEXT, `onSale` INTEGER NOT NULL, `attributes` TEXT NOT NULL, `coinTypeId` TEXT, `value` TEXT, `contract_address` TEXT NOT NULL, `contract_type` TEXT NOT NULL, `external_link` TEXT, `permalink` TEXT, PRIMARY KEY(`accountId`, `tokenId`, `contract_address`), FOREIGN KEY(`accountId`) REFERENCES `AccountRecord`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")
    }
}
