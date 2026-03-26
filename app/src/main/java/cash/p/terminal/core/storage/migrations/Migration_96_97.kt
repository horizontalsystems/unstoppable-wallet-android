package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_96_97 : Migration(96, 97) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS PoisonAddress (
                address TEXT NOT NULL,
                blockchainTypeUid TEXT NOT NULL,
                type TEXT NOT NULL,
                PRIMARY KEY (address, blockchainTypeUid)
            )
        """.trimIndent())
    }
}
