package io.horizontalsystems.walletkit.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_85_86 : Migration(85, 86) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `NftCollectionRecord`")
        db.execSQL("DROP TABLE IF EXISTS `NftAssetRecord`")
        db.execSQL("DROP TABLE IF EXISTS `NftMetadataSyncRecord`")
        db.execSQL("DROP TABLE IF EXISTS `NftAssetBriefMetadataRecord`")
    }
}
