package cash.p.terminal.modules.manageaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.AccountType
import cash.p.terminal.modules.balance.headerNote
import cash.p.terminal.modules.manageaccount.ManageAccountModule.KeyAction
import io.reactivex.disposables.CompositeDisposable

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
            keyActions = getKeyActions(account)
        )
    )
        private set

    private var newName = account.name
    private val disposable = CompositeDisposable()

    init {
        accountManager.accountsFlowable
            .subscribeIO { handleUpdatedAccounts(it) }
            .let { disposable.add(it) }
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

    override fun onCleared() {
        disposable.clear()
    }

    private fun getKeyActions(account: Account): List<KeyAction> {
        if (!account.isBackedUp) {
            return listOf(KeyAction.Backup)
        }
        return when (account.type) {
            is AccountType.Mnemonic -> listOf(
                KeyAction.RecoveryPhrase,
                KeyAction.PrivateKeys,
                KeyAction.PublicKeys
            )
            is AccountType.EvmPrivateKey -> listOf(
                KeyAction.PrivateKeys,
                KeyAction.PublicKeys
            )
            is AccountType.EvmAddress -> listOf(
                KeyAction.PublicKeys
            )
            is AccountType.SolanaAddress -> listOf(
                KeyAction.PublicKeys
            )
            is AccountType.HdExtendedKey -> {
                if (account.type.hdExtendedKey.info.isPublic) {
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
            viewState.copy(keyActions = getKeyActions(account))
        } else {
            viewState.copy(closeScreen = true)
        }
    }

}
