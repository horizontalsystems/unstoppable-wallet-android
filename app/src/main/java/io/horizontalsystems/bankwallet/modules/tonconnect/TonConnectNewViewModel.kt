package io.horizontalsystems.bankwallet.modules.tonconnect

import androidx.lifecycle.viewModelScope
import com.tonapps.wallet.data.tonconnect.entities.DAppManifestEntity
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import com.tonapps.wallet.data.tonconnect.entities.reply.DAppConnectEventError
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.toTonWalletFullAccess
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TonConnectNewViewModel(
    private val requestEntity: DAppRequestEntity,
) : ViewModelUiState<TonConnectNewUiState>() {
    private val tonConnectKit = App.tonConnectManager.kit

    private var manifest: DAppManifestEntity? = null
    private var accounts: List<Account> = listOf()
    private var account = App.accountManager.activeAccount
    private var finish = false
    private var error: Throwable? = null
    private var toast: String? = null

    override fun createState() = TonConnectNewUiState(
        manifest = manifest,
        accounts = accounts,
        account = account,
        finish = finish,
        error = error,
        toast = toast,
    )

    init {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                manifest = tonConnectKit.getManifest(requestEntity.payload.manifestUrl)
                emitState()
            } catch (e: Throwable) {
                error = NoManifestError()
                emitState()
            }
        }

        accounts = App.accountManager.accounts.filter {
            it.type is AccountType.Mnemonic
        }

        if (accounts.isEmpty()) {
            error = NoTonAccountError()
            emitState()
        }
    }

    fun onSelectAccount(account: Account) {
        this.account = account
        emitState()
    }

    fun connect() {
        viewModelScope.launch {
            try {
                val manifest = manifest ?: throw NoManifestError()
                val account = account ?: throw IllegalArgumentException("Empty account")

                tonConnectKit.connect(
                    requestEntity,
                    manifest,
                    account.id,
                    account.type.toTonWalletFullAccess()
                )
                finish = true
            } catch (e: Throwable) {
                toast = e.message?.nullIfBlank() ?: e.javaClass.simpleName
            }

            emitState()
        }
    }

    fun reject() {
        viewModelScope.launch {
            try {
                val manifest = manifest ?: throw NoManifestError()
                val account = account ?: throw IllegalArgumentException("Empty account")
                val dapp = tonConnectKit.newApp(
                    manifest,
                    account.id,
                    false,
                    requestEntity.id,
                    account.id,
                    false
                )

                val error = DAppConnectEventError(
                    id = System.currentTimeMillis().toString(),
                    errorCode = 300,
                    errorMessage = "User declined the transaction"
                )

                tonConnectKit.send(dapp, error.toJSON())
                finish = true
            } catch (e: Throwable) {
                toast = e.message?.nullIfBlank() ?: e.javaClass.simpleName
            }
            emitState()
        }
    }

    fun onToastShow() {
        toast = null
        emitState()
    }
}

sealed class TonConnectError : Error()
class NoManifestError : TonConnectError()
class NoTonAccountError : TonConnectError()

data class TonConnectNewUiState(
    val manifest: DAppManifestEntity?,
    val accounts: List<Account>,
    val account: Account?,
    val finish: Boolean,
    val error: Throwable?,
    val toast: String?
) {
    val connectEnabled get() = error == null && account != null
}
