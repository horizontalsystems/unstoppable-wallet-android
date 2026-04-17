package cash.p.terminal.domain.usecase

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import cash.p.terminal.core.App
import io.horizontalsystems.core.DispatcherProvider
import cash.p.terminal.core.storage.AppDatabase
import cash.p.terminal.core.tor.torcore.TorConstants
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.walletconnect.WCDelegate
import cash.p.terminal.strings.helpers.LocaleHelper
import cash.p.terminal.wallet.IAccountCleaner
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.widgets.MarketWidget
import cash.p.terminal.widgets.MarketWidgetStateDefinition
import cash.p.terminal.widgets.MarketWidgetWorker
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import androidx.core.content.edit
import cash.p.terminal.core.ILocalStorage

class ResetUseCase(
    private val context: Context,
    private val localStorage: ILocalStorage,
    private val appDatabase: AppDatabase,
    private val accountManager: IAccountManager,
    private val accountCleaner: IAccountCleaner,
    private val contactsRepository: ContactsRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val glanceManager: GlanceAppWidgetManager
) {

    suspend operator fun invoke() {
        withContext(dispatcherProvider.io) {
            haltWalletConnect()

            runCatching {
                val accountIds = accountManager.accounts.map { it.id }
                if (accountIds.isNotEmpty()) {
                    accountCleaner.clearAccounts(accountIds)
                }
            }.onFailure { Timber.w(it, "Failed clearing account artifacts") }

            runCatching {
                val mainShowedOnce = localStorage.mainShowedOnce
                val appIcon = localStorage.appIcon
                val isSystemPinRequired = localStorage.isSystemPinRequired

                App.keyStoreManager.resetApp("InvalidKey")

                localStorage.appIcon = appIcon
                localStorage.mainShowedOnce = mainShowedOnce
                localStorage.isSystemPinRequired = isSystemPinRequired
            }.onFailure { Timber.w(it, "Failed resetting keystore") }

            purgeDatabases()
            purgeLocalePreferences()
            purgeFilesAndCaches()
        }
    }

    private fun haltWalletConnect() {
        runCatching {
            WCDelegate.getActiveSessions().forEach { session ->
                WCDelegate.deleteSession(session.topic)
            }
            WCDelegate.deleteAllPairings()
        }.onFailure { Timber.w(it, "Failed clearing WalletConnect sessions") }
    }

    private fun purgeDatabases() {
        runCatching { appDatabase.clearAllTables() }
            .onFailure { Timber.w(it, "Failed clearing app database") }

        runCatching { context.deleteDatabase(PREMIUM_DB_NAME) }
            .onFailure { Timber.w(it, "Failed deleting premium database file") }

        runCatching { context.deleteDatabase(CACHE_DB_NAME) }
            .onFailure { Timber.w(it, "Failed deleting cache database file") }

        runCatching { context.deleteDatabase(LOGGING_DB_NAME) }
            .onFailure { Timber.w(it, "Failed deleting logging database file") }
    }

    private fun purgeLocalePreferences() {
        runCatching {
            context.getSharedPreferences(LocaleHelper::class.java.name, Context.MODE_PRIVATE)
                .edit {
                    clear()
                }
        }.onFailure { Timber.w(it, "Failed clearing locale prefs") }
    }

    private suspend fun purgeFilesAndCaches() {
        runCatching {
            contactsRepository.clear()
            File(context.filesDir, CONTACTS_FILE_NAME).delete()
        }.onFailure { Timber.w(it, "Failed clearing contacts") }

        runCatching { clearWidgetState() }
            .onFailure { Timber.w(it, "Failed clearing widget state") }

        runCatching {
            context.getDir(TorConstants.DIRECTORY_TOR_DATA, Context.MODE_PRIVATE)
                .deleteRecursively()
        }.onFailure { Timber.w(it, "Failed clearing Tor data") }

        runCatching {
            File(context.filesDir, PHOTOS_DIR_NAME).deleteRecursively()
        }.onFailure { Timber.w(it, "Failed clearing login photos") }
    }

    private suspend fun clearWidgetState() {
        val glanceIds = runCatching { glanceManager.getGlanceIds(MarketWidget::class.java) }
            .getOrElse { emptyList() }

        glanceIds.forEach { glanceId ->
            runCatching {
                val file = MarketWidgetStateDefinition.getLocation(context, glanceId.toString())
                if (file.exists()) {
                    file.delete()
                }
            }.onFailure { Timber.w(it, "Failed deleting widget state for $glanceId") }
        }

        MarketWidgetWorker.cancel(context)
    }

    companion object {
        private const val CONTACTS_FILE_NAME = "UW_Contacts.json"
        private const val PREMIUM_DB_NAME = "premium_database"
        private const val CACHE_DB_NAME = "db_cache"
        private const val LOGGING_DB_NAME = "logging_database"
        private const val PHOTOS_DIR_NAME = "login_photos"
    }
}
