package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_67_68 : Migration(67, 68) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE `HardwarePublicKey` ADD COLUMN `type` INTEGER NOT NULL DEFAULT 0
        """
        )
    }
}
