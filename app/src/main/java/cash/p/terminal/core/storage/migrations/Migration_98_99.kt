package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_98_99 : Migration(98, 99) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE PendingMultiSwap ADD COLUMN accountId TEXT NOT NULL DEFAULT ''")
    }
}
