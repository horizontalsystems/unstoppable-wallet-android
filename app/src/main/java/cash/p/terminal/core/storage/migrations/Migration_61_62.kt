package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_61_62 : Migration(61, 62) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE EnabledWalletCache ADD COLUMN stackingUnpaid TEXT NOT NULL DEFAULT '0'")
    }
}
