package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_64_65 : Migration(64, 65) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create temporary table with new schema
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS EnabledWallet_temp (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tokenQueryId TEXT NOT NULL,
                accountId TEXT NOT NULL,
                walletOrder INTEGER,
                coinName TEXT,
                coinCode TEXT,
                coinDecimals INTEGER,
                coinImage TEXT,
                FOREIGN KEY (accountId) REFERENCES AccountRecord(id) 
                ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
            )
        """)

        // 2. Copy data from old table to temp table
        db.execSQL("""
            INSERT INTO EnabledWallet_temp (
                tokenQueryId, 
                accountId, 
                walletOrder, 
                coinName, 
                coinCode, 
                coinDecimals, 
                coinImage
            )
            SELECT 
                tokenQueryId, 
                accountId, 
                walletOrder, 
                coinName, 
                coinCode, 
                coinDecimals, 
                coinImage 
            FROM EnabledWallet
        """)

        // 3. Drop the old table
        db.execSQL("DROP TABLE EnabledWallet")

        // 4. Rename temp table to original name
        db.execSQL("ALTER TABLE EnabledWallet_temp RENAME TO EnabledWallet")

        // 5. Recreate the index
        db.execSQL("CREATE INDEX IF NOT EXISTS index_EnabledWallet_accountId ON EnabledWallet(accountId)")
    }
}
