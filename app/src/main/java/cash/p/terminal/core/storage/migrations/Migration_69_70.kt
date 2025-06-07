package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_69_70 : Migration(69, 70) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE HardwarePublicKey ADD COLUMN derivationPath TEXT NOT NULL DEFAULT ''"
        )
    }
}
