package cash.p.terminal.wallet.storage

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object Migration_15_16 : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_TokenEntity_blockchainUid_type_reference
            ON TokenEntity(blockchainUid, type, reference)
            """.trimIndent()
        )
    }
}
