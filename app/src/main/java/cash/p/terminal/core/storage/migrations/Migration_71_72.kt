package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_71_72 : Migration(71, 72) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "DELETE FROM EnabledWallet WHERE tokenQueryId='binancecoin|native'"
        )
    }
}
