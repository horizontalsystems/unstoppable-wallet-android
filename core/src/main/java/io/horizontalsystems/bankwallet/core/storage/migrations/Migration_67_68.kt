package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_67_68 : Migration(67, 68) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE MoneroNodeRecord ADD COLUMN `trusted` INTEGER NOT NULL DEFAULT 1")
    }
}
