package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Keeps hashless pending rows available for transaction matching after balance-side confirmation.
 */
object Migration_102_103 : Migration(102, 103) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE PendingTransaction ADD COLUMN balanceConfirmedAt INTEGER")
    }
}
