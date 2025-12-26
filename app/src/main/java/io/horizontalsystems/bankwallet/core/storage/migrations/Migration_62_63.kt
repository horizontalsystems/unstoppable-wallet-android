package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_62_63 : Migration(62, 63) {
    override fun migrate(db: SupportSQLiteDatabase) {
        //clean binancecoin coins and tokens from wallet
        db.execSQL("DELETE FROM EnabledWallet WHERE tokenQueryId LIKE '%binancecoin%'")
        db.execSQL("DELETE FROM EnabledWalletCache WHERE tokenQueryId LIKE '%binancecoin%'")
    }
}
