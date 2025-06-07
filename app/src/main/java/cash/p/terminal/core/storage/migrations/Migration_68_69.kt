package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_68_69 : Migration(68, 69) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE HardwarePublicKey ADD COLUMN publicKey BLOB NOT NULL DEFAULT ''".trimIndent()
        )
    }
}
