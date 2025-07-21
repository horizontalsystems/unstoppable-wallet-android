package cash.p.terminal.modules.manageaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.modules.balance.headerNote
import cash.p.terminal.modules.manageaccount.ManageAccountModule.BackupItem
import cash.p.terminal.modules.manageaccount.ManageAccountModule.KeyAction
import cash.p.terminal.tangem.domain.sdk.TangemSdkManager
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.IAccountManager
import com.m2049r.xmrwallet.service.MoneroWalletService
import com.tangem.common.card.Card
import com.tangem.common.doOnSuccess
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.koin.java.KoinJavaComponent.inject

class ManageAccountViewModel(
    accountId: String,
    private val accountManager: IAccountManager,
) : ViewModel() {

    val account: Account = accountManager.account(accountId)!!
    private val tangemSdkManager: TangemSdkManager by inject(TangemSdkManager::class.java)

    var viewState by mutableStateOf(
        ManageAccountModule.ViewState(
            title = account.name,
            newName = account.name,
            canSave = false,
            closeScreen = false,
            headerNote = account.headerNote(false),
            keyActions = getKeyActions(account),
            backupActions = getBackupItems(account),
            signedHashes = (account.type as? AccountType.HardwareCard)?.signedHashes
        )
    )
        private set

    private var newName = account.name

    private val _showAccessCodeRecoveryDialog = Channel<Card>(Channel.UNLIMITED)
    val showAccessCodeRecoveryDialog = _showAccessCodeRecoveryDialog.receiveAsFlow()

    init {
        viewModelScope.launch {
            accountManager.accountsFlowable.asFlow()
                .collect { handleUpdatedAccounts(it) }
        }
    }

    private fun changeAccessCode() = viewModelScope.launch {
        tangemSdkManager.setAccessCode(null)
    }

    private fun accessCodeRecovery() = viewModelScope.launch {
        tangemSdkManager.scanCard(cardId = null, allowRequestAccessCodeFromRepository = false)
            .doOnSuccess {
                _showAccessCodeRecoveryDialog.trySend(it)
            }
    }

    private fun forgotAccessCode() = viewModelScope.launch {
        val accessType = account.type as? AccountType.HardwareCard ?: return@launch
        tangemSdkManager.restoreAccessCode(accessType.cardId)
    }

    fun onActionClick(action: KeyAction) {
        when (action) {
            KeyAction.ResetToFactorySettings -> deleteAccount()
            KeyAction.ChangeAccessCode -> changeAccessCode()
            KeyAction.AccessCodeRecovery -> accessCodeRecovery()
            KeyAction.ForgotAccessCode -> forgotAccessCode()

            KeyAction.RecoveryPhrase,
            KeyAction.PublicKeys,
            KeyAction.ViewKey,
            KeyAction.SpendKey,
            KeyAction.PrivateKeys -> Unit
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

    private fun deleteAccount() = viewModelScope.launch {
        accountManager.delete(account.id)
        viewState = viewState.copy(closeScreen = true)
    }

    private fun getBackupItems(account: Account): List<BackupItem> {
        if (account.isWatchAccount) {
            return emptyList()
        }
        if (account.type is AccountType.HdExtendedKey
            || account.type is AccountType.EvmPrivateKey
            || account.type is AccountType.StellarSecretKey
        ) {
            return listOf(BackupItem.LocalBackup(false))
        }

        val items = mutableListOf<BackupItem>()
        if (account.accountSupportsBackup) {
            if (!account.isBackedUp && !account.isFileBackedUp) {
                items.add(BackupItem.ManualBackup(true))
                items.add(BackupItem.LocalBackup(true))
                items.add(BackupItem.InfoText(R.string.BackupRecoveryPhrase_BackupRequiredText))
            } else {
                items.add(
                    BackupItem.ManualBackup(
                        showAttention = !account.isBackedUp,
                        completed = account.isBackedUp
                    )
                )
                items.add(BackupItem.LocalBackup(false))
                items.add(BackupItem.InfoText(R.string.BackupRecoveryPhrase_BackupRecomendedText))
            }
        }

        return items
    }

    private fun getKeyActions(account: Account): List<KeyAction> {
        if (!account.isBackedUp && !account.isFileBackedUp && account.accountSupportsBackup) {
            return emptyList()
        }
        return when (account.type) {
            is AccountType.Mnemonic -> listOf(
                KeyAction.RecoveryPhrase,
                KeyAction.PrivateKeys,
                KeyAction.PublicKeys,
            )

            is AccountType.MnemonicMonero -> listOf(
                KeyAction.RecoveryPhrase,
                KeyAction.ViewKey,
                KeyAction.SpendKey,
            )

            is AccountType.EvmPrivateKey -> listOf(
                KeyAction.PrivateKeys,
                KeyAction.PublicKeys,
            )

            is AccountType.StellarSecretKey -> listOf(
                KeyAction.PrivateKeys
            )

            is AccountType.HardwareCard -> listOf(
                KeyAction.AccessCodeRecovery,
                KeyAction.ChangeAccessCode,
                KeyAction.ForgotAccessCode,
                KeyAction.ResetToFactorySettings
            )

            is AccountType.ZCashUfvKey,
            is AccountType.EvmAddress,
            is AccountType.SolanaAddress,
            is AccountType.TronAddress,
            is AccountType.TonAddress,
            is AccountType.StellarAddress,
            is AccountType.BitcoinAddress -> listOf()

            is AccountType.HdExtendedKey -> {
                if ((account.type as AccountType.HdExtendedKey).hdExtendedKey.isPublic) {
                    listOf(KeyAction.PublicKeys)
                } else {
                    listOf(KeyAction.PrivateKeys, KeyAction.PublicKeys)
                }
            }
        }
    }

    fun getMoneroViewKey(): String {
        // We have only one active Monero wallet, so MoneroWalletService is enough to get active wallet
        val moneroWalletService: MoneroWalletService by inject(MoneroWalletService::class.java)
        return moneroWalletService.wallet?.secretViewKey ?: ""
    }

    fun getMoneroSpendKey(): String {
        // We have only one active Monero wallet, so MoneroWalletService is enough to get active wallet
        val moneroWalletService: MoneroWalletService by inject(MoneroWalletService::class.java)
        return moneroWalletService.wallet?.secretSpendKey ?: ""
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
