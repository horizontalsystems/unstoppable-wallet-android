package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_99_100 : Migration(99, 100) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS PoisonAddress")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS PoisonAddress (
                address TEXT NOT NULL,
                blockchainTypeUid TEXT NOT NULL,
                accountId TEXT NOT NULL,
                type TEXT NOT NULL,
                PRIMARY KEY (address, blockchainTypeUid, accountId)
            )
        """.trimIndent())
    }
}
