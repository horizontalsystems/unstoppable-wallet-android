package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_92_93 : Migration(92, 93) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // expectedAmountOut was added to Migration_91_92's CREATE TABLE.
        // This migration handles devices that already ran 91->92 without the column.
        val cursor = db.query("PRAGMA table_info(PendingMultiSwap)")
        val columns = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()
        if ("expectedAmountOut" !in columns) {
            db.execSQL("ALTER TABLE PendingMultiSwap ADD COLUMN expectedAmountOut TEXT NOT NULL DEFAULT '0'")
        }
    }
}
