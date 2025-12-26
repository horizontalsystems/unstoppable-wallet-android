package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage

class MigrationManager(
    private val localStorage: ILocalStorage,
    private val termsManager: TermsManager,
) {

    private val latestMigrationVersion = 1

    fun runMigrations() {
        val lastMigrationVersion = localStorage.lastMigrationVersion

        if (lastMigrationVersion == null && !localStorage.termsAccepted) {
            // Fresh install: No migrations to run, just set the version to the latest
            localStorage.lastMigrationVersion = latestMigrationVersion
            return
        }

        if ((lastMigrationVersion ?: 0) < 1) {
            termsManager.migrateToTermsV2()
        }

        // --- Template for Future Migrations ---
        // if (lastMigrationVersion ?: 0 < 2) {
        //     someOtherManager.migrateSomething()
        // }

        localStorage.lastMigrationVersion = latestMigrationVersion
    }
}
