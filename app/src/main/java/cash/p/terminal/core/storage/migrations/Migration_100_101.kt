package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_100_101 : Migration(100, 101) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE PoisonAddress ADD COLUMN sendCount INTEGER NOT NULL DEFAULT 1")
    }
}
