package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_48_49 : Migration(48, 49) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `NftAssetBriefMetadataRecord` (`nftUid` TEXT NOT NULL, `providerCollectionUid` TEXT NOT NULL, `name` TEXT, `imageUrl` TEXT, `previewImageUrl` TEXT, PRIMARY KEY(`nftUid`))")    }
}