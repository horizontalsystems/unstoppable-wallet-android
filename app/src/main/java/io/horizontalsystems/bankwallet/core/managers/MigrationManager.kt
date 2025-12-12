package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsModule

class MigrationManager(
    private val localStorage: ILocalStorage,
    private val termsManager: ITermsManager,
) {

    private val latestMigrationVersion = 2

    fun runMigrations() {
        val lastMigrationVersion = localStorage.lastMigrationVersion

        if (lastMigrationVersion == null && !localStorage.termsAccepted) {
            // Fresh install: No migrations to run, just set the version to the latest
            localStorage.lastMigrationVersion = latestMigrationVersion
            return
        }

        if ((lastMigrationVersion ?: 0) < 1) {
            migrateToTermsV2()
        }

        if ((lastMigrationVersion ?: 0) < 2) {
            migrateToTermsV3()
        }

        // --- Template for Future Migrations ---
        // if (lastMigrationVersion ?: 0 < 2) {
        //     someOtherManager.migrateSomething()
        // }

        localStorage.lastMigrationVersion = latestMigrationVersion
    }

    private fun migrateToTermsV2() {
        if(localStorage.termsAccepted) {
            localStorage.termsAccepted = false

            val initialChecked = listOf(
                TermsModule.TermType.Backup.key,
                TermsModule.TermType.DisablingPin.key,
            )
            termsManager.broadcastTermsAccepted(false)
            localStorage.checkedTerms = initialChecked
        }
    }

    private fun migrateToTermsV3() {
        if(localStorage.termsAccepted) {
            localStorage.termsAccepted = false
            termsManager.broadcastTermsAccepted(false)
        }
    }
}
