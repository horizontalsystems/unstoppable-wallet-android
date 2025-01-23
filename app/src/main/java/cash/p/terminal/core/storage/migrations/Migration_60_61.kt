package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_60_61 : Migration(60, 61) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `ChangeNowTransaction` (`date` INTEGER NOT NULL, `transactionId` TEXT NOT NULL, `coinUidIn` TEXT NOT NULL, `blockchainTypeIn` TEXT NOT NULL, `amountIn` TEXT NOT NULL, `addressIn` TEXT NOT NULL, `coinUidOut` TEXT NOT NULL, `blockchainTypeOut` TEXT NOT NULL, `amountOut` TEXT NOT NULL, `addressOut` TEXT NOT NULL, PRIMARY KEY(`date`))")
    }
}
