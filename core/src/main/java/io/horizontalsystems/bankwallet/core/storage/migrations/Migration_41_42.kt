package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_41_42 : Migration(41, 42) {
    override fun migrate(db: SupportSQLiteDatabase) {
        //clean LogEntry table from WalletConnect logs
        db.execSQL("DELETE FROM LogEntry WHERE actionId LIKE 'WalletConnect%'")
    }
}
