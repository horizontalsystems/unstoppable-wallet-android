package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_70_71 : Migration(70, 71) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE HardwarePublicKey ADD COLUMN tokenType TEXT NOT NULL DEFAULT ''"
        )
    }
}
