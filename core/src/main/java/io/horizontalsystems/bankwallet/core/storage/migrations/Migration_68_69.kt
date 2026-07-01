package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_68_69 : Migration(68, 69) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE `EnabledWalletCache_new` (
                `tokenQueryId` TEXT NOT NULL, 
                `accountId` TEXT NOT NULL, 
                `balanceData` TEXT, 
                PRIMARY KEY(`tokenQueryId`, `accountId`),
                FOREIGN KEY(`accountId`) 
                REFERENCES `AccountRecord`(`id`) 
                ON UPDATE CASCADE 
                ON DELETE CASCADE
                DEFERRABLE INITIALLY DEFERRED -- Add this line
            )
        """)
        db.execSQL("DROP TABLE `EnabledWalletCache`")
        db.execSQL("ALTER TABLE `EnabledWalletCache_new` RENAME TO `EnabledWalletCache`")
    }
}
