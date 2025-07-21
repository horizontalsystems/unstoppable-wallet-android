package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_73_74 : Migration(73, 74) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE HardwarePublicKey ADD COLUMN derivedPublicKey BLOB NOT NULL DEFAULT ''")
    }
}
