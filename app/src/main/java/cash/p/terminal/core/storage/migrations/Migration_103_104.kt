package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_103_104 : Migration(103, 104) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE SwapProviderTransaction ADD COLUMN accountId TEXT NOT NULL DEFAULT ''"
        )
    }
}
