package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_59_60 : Migration(59, 60) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE EnabledWallet ADD `coinImage` TEXT")
    }
}
