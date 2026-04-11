package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_97_98 : Migration(97, 98) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM PoisonAddress WHERE type = 'SCAM'")
    }
}
