package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_80_81 : Migration(80, 81) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `ZcashEndpointRecord` (
                `url` TEXT NOT NULL,
                PRIMARY KEY(`url`)
            )
        """.trimIndent())
    }
}
