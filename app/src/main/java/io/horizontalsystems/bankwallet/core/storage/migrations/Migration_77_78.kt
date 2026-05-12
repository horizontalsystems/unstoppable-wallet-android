package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_77_78 : Migration(77, 78) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `ZanoNodeRecord` (
                `url` TEXT NOT NULL,
                PRIMARY KEY(`url`)
            )
        """.trimIndent())
    }
}
