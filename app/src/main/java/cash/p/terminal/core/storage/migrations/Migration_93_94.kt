package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_93_94 : Migration(93, 94) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE PendingMultiSwap ADD COLUMN leg2StartedAt INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE PendingMultiSwap ADD COLUMN completedAt INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE PendingMultiSwap ADD COLUMN leg1ProviderTransactionId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE PendingMultiSwap ADD COLUMN leg2ProviderTransactionId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE PendingMultiSwap ADD COLUMN leg1InfoRecordUid TEXT DEFAULT NULL")
    }
}
