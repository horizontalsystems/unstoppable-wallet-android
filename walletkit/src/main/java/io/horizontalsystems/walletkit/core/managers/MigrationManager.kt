package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.ILocalStorage

class MigrationManager(
    private val localStorage: ILocalStorage,
) {

    private val latestMigrationVersion = 4

    fun runMigrations() {
        val lastMigrationVersion = localStorage.lastMigrationVersion ?: 0

        // Does nothing on a fresh install, which has no old setting to convert
        if (lastMigrationVersion < 4) {
            localStorage.migrateEnabledPaidActionsFromDisabled()
        }

        // --- Template for Future Migrations ---
        // if (lastMigrationVersion < 5) {
        //     someOtherManager.migrateSomething()
        // }

        localStorage.lastMigrationVersion = latestMigrationVersion
    }
}
