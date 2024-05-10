package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_39_40 : Migration(39, 40) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE EnabledWallet ADD `coinName` TEXT")
        db.execSQL("ALTER TABLE EnabledWallet ADD `coinCode` TEXT")
        db.execSQL("ALTER TABLE EnabledWallet ADD `coinDecimals` INTEGER")

        db.execSQL(
            "UPDATE EnabledWallet " +
                    "SET coinName = (SELECT coinName FROM CustomToken WHERE CustomToken.coinType = EnabledWallet.coinId), " +
                    "coinCode = (SELECT coinCode FROM CustomToken WHERE CustomToken.coinType = EnabledWallet.coinId), " +
                    "coinDecimals = (SELECT decimal FROM CustomToken WHERE CustomToken.coinType = EnabledWallet.coinId) " +
                    "WHERE EXISTS (SELECT * FROM CustomToken WHERE CustomToken.coinType = EnabledWallet.coinId)"
        )

        db.execSQL("DELETE FROM CustomToken")
    }
}
