package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_46_47 : Migration(46, 47) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE EvmAccountState ADD `restored` INTEGER NOT NULL DEFAULT 0")
    }
}
