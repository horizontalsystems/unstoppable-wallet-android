package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_57_58 : Migration(57, 58) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE `ActiveAccount_new` (`level` INTEGER NOT NULL, `accountId` TEXT NOT NULL, PRIMARY KEY(`level`))")
        db.execSQL("INSERT INTO ActiveAccount_new (`accountId`, `level`) SELECT `accountId`, 0 FROM ActiveAccount LIMIT 0, 1")
        db.execSQL("DROP TABLE ActiveAccount")
        db.execSQL("ALTER TABLE ActiveAccount_new RENAME TO ActiveAccount")
    }
}
