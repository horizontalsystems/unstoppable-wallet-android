package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_65_66 : Migration(65, 66) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `MoneroNodeRecord` (`url` TEXT NOT NULL, `username` TEXT, `password` TEXT, PRIMARY KEY(`url`))")
    }
}
