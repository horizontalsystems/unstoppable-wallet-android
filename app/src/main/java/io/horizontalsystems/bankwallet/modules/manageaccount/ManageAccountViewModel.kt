package io.horizontalsystems.bankwallet.modules.manageaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.balance.headerNote
import io.horizontalsystems.bankwallet.modules.manageaccount.ManageAccountModule.BackupItem
import io.horizontalsystems.bankwallet.modules.manageaccount.ManageAccountModule.KeyAction
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class ManageAccountViewModel(
    accountId: String,
    private val accountManager: IAccountManager
) : ViewModel() {

    val account: Account = accountManager.account(accountId)!!

    var viewState by mutableStateOf(
        ManageAccountModule.ViewState(
            title = account.name,
            newName = account.name,
            canSave = false,
            closeScreen = false,
            headerNote = account.headerNote(false),
            keyActions = getKeyActions(account),
            backupActions = getBackupItems(account),
        )
    )
        private set

    private var newName = account.name

    init {
        viewModelScope.launch {
            accountManager.accountsFlowable.asFlow()
                .collect { handleUpdatedAccounts(it) }
        }
    }

    fun onChange(name: String) {
        newName = name.trim().replace("\n", " ")
        val canSave = newName != account.name && newName.isNotEmpty()
        viewState = viewState.copy(
            canSave = canSave,
            newName = newName
        )
    }

    fun onSave() {
        val account = account.copy(name = newName)
        accountManager.update(account)
        viewState = viewState.copy(closeScreen = true)
    }

    fun onClose() {
        viewState = viewState.copy(closeScreen = false)
    }

    private fun getBackupItems(account: Account): List<BackupItem> {
        if (account.isWatchAccount) {
            return emptyList()
        }
        if (account.type is AccountType.HdExtendedKey || account.type is AccountType.EvmPrivateKey) {
            return listOf(BackupItem.LocalBackup(false))
        }

        val items = mutableListOf<BackupItem>()
        if (!account.isBackedUp && !account.isFileBackedUp) {
            items.add(BackupItem.ManualBackup(true))
            items.add(BackupItem.LocalBackup(true))
            items.add(BackupItem.InfoText(R.string.BackupRecoveryPhrase_BackupRequiredText))
        } else {
            items.add(BackupItem.ManualBackup(showAttention = !account.isBackedUp, completed = account.isBackedUp))
            items.add(BackupItem.LocalBackup(false))
            items.add(BackupItem.InfoText(R.string.BackupRecoveryPhrase_BackupRecomendedText))
        }

        return items
    }

    private fun getKeyActions(account: Account): List<KeyAction> {
        if (!account.isBackedUp && !account.isFileBackedUp) {
            return emptyList()
        }
        return when (account.type) {
            is AccountType.Mnemonic -> listOf(
                KeyAction.RecoveryPhrase,
                KeyAction.PrivateKeys,
                KeyAction.PublicKeys,
            )

            is AccountType.EvmPrivateKey -> listOf(
                KeyAction.PrivateKeys,
                KeyAction.PublicKeys,
            )
            is AccountType.EvmAddress -> listOf()
            is AccountType.SolanaAddress -> listOf()
            is AccountType.TronAddress -> listOf()
            is AccountType.HdExtendedKey -> {
                if (account.type.hdExtendedKey.isPublic) {
                    listOf(KeyAction.PublicKeys)
                } else {
                    listOf(KeyAction.PrivateKeys, KeyAction.PublicKeys)
                }
            }
        }
    }

    private fun handleUpdatedAccounts(accounts: List<Account>) {
        val account = accounts.find { it.id == account.id }
        viewState = if (account != null) {
            viewState.copy(
                keyActions = getKeyActions(account),
                backupActions = getBackupItems(account)
            )
        } else {
            viewState.copy(closeScreen = true)
        }
    }

}
