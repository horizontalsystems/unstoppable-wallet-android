package io.horizontalsystems.bankwallet.modules.restorelocal

import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statAccountType
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.backuplocal.BackupLocalModule.WalletBackup
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.BackupProvider
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.BackupSection
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.BackupViewItemFactory
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.DecryptedFullBackup
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.FullBackup
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.RestoreException
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.SelectBackupItemsViewModel.OtherBackupViewItem
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.SelectBackupItemsViewModel.WalletBackupViewItem
import io.horizontalsystems.bankwallet.modules.restorelocal.RestoreLocalModule.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel(assistedFactory = RestoreLocalViewModel.Factory::class)
class RestoreLocalViewModel @AssistedInject constructor(
    @Assisted("backupJsonString") private val backupJsonString: String?,
    @Assisted private val statPage: StatPage,
    @Assisted("fileName") fileName: String?,
    private val accountFactory: IAccountFactory,
    private val backupProvider: BackupProvider,
    private val backupViewItemFactory: BackupViewItemFactory,
) : ViewModelUiState<UiState>() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("backupJsonString") backupJsonString: String?,
            statPage: StatPage,
            @Assisted("fileName") fileName: String?,
        ): RestoreLocalViewModel
    }

    private var passphrase = ""
    private var passphraseState: DataState.Error? = null
    private var showButtonSpinner = false
    private var walletBackup: WalletBackup? = null
    private var fullBackup: FullBackup? = null
    private var parseError: Exception? = null
    private var showSelectCoins: AccountType? = null
    private var manualBackup = false
    private var restored = false

    private var decryptedFullBackup: DecryptedFullBackup? = null
    private var walletBackupViewItems: List<WalletBackupViewItem> = emptyList()
    private var otherBackupViewItems: List<OtherBackupViewItem> = emptyList()
    private var showBackupItems = false

    val accountName by lazy {
        fileName?.let { name ->
            val processed = name
                .replace(".json", "")
                .replace("UW_Backup_", "")
                .replace("_", " ")
                .trim()
            if (processed.isNotBlank()) return@lazy processed
        }
        accountFactory.getNextAccountName()
    }

    val displayFileName: String? = fileName
        ?.removeSuffix(".json")
        ?.replace(Regex("_\\d{4}-\\d{2}-\\d{2}$"), "")
        ?.replace("_", " ")

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val gson = GsonBuilder()
                    .disableHtmlEscaping()
                    .enableComplexMapKeySerialization()
                    .create()

                fullBackup = try {
                    val backup = gson.fromJson(backupJsonString, FullBackup::class.java)
                    backup.settings.language // if single walletBackup it will throw exception
                    backup
                } catch (_: Exception) {
                    null
                }

                walletBackup = gson.fromJson(backupJsonString, WalletBackup::class.java)
                manualBackup = walletBackup?.manualBackup ?: false
            } catch (e: Exception) {
                parseError = e
                emitState()
            }
        }
    }

    override fun createState() = UiState(
        passphraseState = passphraseState,
        showButtonSpinner = showButtonSpinner,
        parseError = parseError,
        showSelectCoins = showSelectCoins,
        manualBackup = manualBackup,
        restored = restored,
        walletBackupViewItems = walletBackupViewItems,
        otherBackupViewItems = otherBackupViewItems,
        showBackupItems = showBackupItems,
        hasSelection = walletBackupViewItems.any { it.selected } ||
            otherBackupViewItems.any { it.selected && it.section != null }
    )

    fun onChangePassphrase(v: String) {
        passphrase = v
        passphraseState = null
        emitState()
    }

    fun onImportClick() {
        when {
            fullBackup != null -> {
                fullBackup?.let { showFullBackupItems(it) }
            }

            walletBackup != null -> {
                walletBackup?.let { restoreSingleWallet(it, accountName) }
            }
        }
    }

    private fun showFullBackupItems(it: FullBackup): Job {
        showButtonSpinner = true
        emitState()

        return viewModelScope.launch(Dispatchers.IO) {
            try {
                val decrypted = backupProvider.decryptedFullBackup(it, passphrase)
                val backupItems = backupProvider.fullBackupItems(decrypted)
                val backupViewItems = backupViewItemFactory.backupViewItems(backupItems)

                walletBackupViewItems = backupViewItems.first
                otherBackupViewItems = backupViewItems.second
                decryptedFullBackup = decrypted
                showBackupItems = true
            } catch (keyException: RestoreException.EncryptionKeyException) {
                parseError = keyException
            } catch (_: RestoreException.InvalidPasswordException) {
                passphraseState = DataState.Error(Exception(Translator.getString(R.string.ImportBackupFile_Error_InvalidPassword)))
            } catch (e: Exception) {
                parseError = e
            } finally {
                withContext(Dispatchers.Main) {
                    showButtonSpinner = false
                    emitState()
                }
            }
        }
    }

    fun toggleWallet(wallet: WalletBackupViewItem) {
        walletBackupViewItems = walletBackupViewItems.map {
            if (it.account.id == wallet.account.id) it.copy(selected = !wallet.selected) else it
        }
        emitState()
    }

    fun toggleOtherItem(item: OtherBackupViewItem) {
        otherBackupViewItems = otherBackupViewItems.map {
            if (it.section != null && it.section == item.section) it.copy(selected = !item.selected) else it
        }
        emitState()
    }

    fun shouldShowReplaceWarning(): Boolean {
        val contactsSelected = otherBackupViewItems.any { it.section == BackupSection.Contacts && it.selected }
        return backupProvider.shouldShowReplaceWarning(decryptedFullBackup) && contactsSelected
    }

    fun restoreFullBackup() {
        decryptedFullBackup?.let { backup ->
            val selectedIds = walletBackupViewItems.filter { it.selected }.map { it.account.id }.toSet()
            val selectedSections = otherBackupViewItems.mapNotNull { if (it.selected) it.section else null }.toSet()
            val filtered = backup.copy(
                wallets = backup.wallets.filter { it.account.id in selectedIds },
                sections = selectedSections
            )
            restoreFullBackup(filtered)
        }
    }

    private fun restoreFullBackup(decryptedFullBackup: DecryptedFullBackup) {
        showButtonSpinner = true
        emitState()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                backupProvider.restoreFullBackup(decryptedFullBackup, passphrase)
                this@RestoreLocalViewModel.decryptedFullBackup = null
                restored = true

                stat(page = statPage, event = StatEvent.ImportFull)
            } catch (keyException: RestoreException.EncryptionKeyException) {
                parseError = keyException
            } catch (_: RestoreException.InvalidPasswordException) {
                passphraseState = DataState.Error(Exception(Translator.getString(R.string.ImportBackupFile_Error_InvalidPassword)))
            } catch (e: Exception) {
                parseError = e
            } finally {
                withContext(Dispatchers.Main) {
                    showButtonSpinner = false
                    emitState()
                }
            }
        }
    }

    private fun restoreSingleWallet(backup: WalletBackup, accountName: String) {
        showButtonSpinner = true
        emitState()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val type = backupProvider.accountType(backup, passphrase)
                if (backup.enabledWallets.isNullOrEmpty()) {
                    showSelectCoins = type
                } else {
                    backupProvider.restoreSingleWalletBackup(type, accountName, backup)
                    restored = true
                }

                stat(page = statPage, event = StatEvent.ImportWallet(type.statAccountType))
            } catch (keyException: RestoreException.EncryptionKeyException) {
                parseError = keyException
            } catch (_: RestoreException.InvalidPasswordException) {
                passphraseState = DataState.Error(Exception(Translator.getString(R.string.ImportBackupFile_Error_InvalidPassword)))
            } catch (e: Exception) {
                parseError = e
            } finally {
                withContext(Dispatchers.Main) {
                    showButtonSpinner = false
                    emitState()
                }
            }
        }
    }

    fun onSelectCoinsShown() {
        showSelectCoins = null
        emitState()
    }

    fun onBackupItemsShown() {
        showBackupItems = false
        emitState()
    }

}
