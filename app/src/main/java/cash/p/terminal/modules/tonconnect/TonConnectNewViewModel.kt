package cash.p.terminal.modules.tonconnect

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.core.DispatcherProvider
import cash.p.terminal.core.managers.toTonWalletFullAccess
import cash.p.terminal.core.storage.HardwarePublicKeyStorage
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.supportsTonConnect
import com.tonapps.wallet.data.tonconnect.entities.DAppManifestEntity
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import io.horizontalsystems.core.ViewModelUiState
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.core.retryWhen
import io.horizontalsystems.tonkit.tonconnect.TonConnectKit
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

class TonConnectNewViewModel(
    private val requestEntity: DAppRequestEntity,
    private val tonConnectKit: TonConnectKit
) : ViewModelUiState<TonConnectNewUiState>() {

    private val hardwarePublicKeyStorage: HardwarePublicKeyStorage by inject(
        HardwarePublicKeyStorage::class.java
    )
    private val accountManager: IAccountManager by inject(IAccountManager::class.java)
    private val dispatchers: DispatcherProvider by inject(DispatcherProvider::class.java)

    private var manifest: DAppManifestEntity? = null
    private var accounts: List<Account> = listOf()
    private var account: Account? = null
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
        accounts = accountManager.accounts.filter {
            it.supportsTonConnect()
        }

        accountManager.activeAccount?.let { activeAccount ->
            accounts.find { it.id == activeAccount.id }?.let {
                account = it
            } ?: run {
                account = accounts.firstOrNull()
            }
        }

        viewModelScope.launch(dispatchers.io) {
            try {
                manifest = retryWhen(times = 3, predicate = { true }) {
                    tonConnectKit.getManifest(requestEntity.payload.manifestUrl)
                }
                emitState()
            } catch (e: Throwable) {
                error = NoManifestError(e.message)
                emitState()
            }
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
        viewModelScope.launch(dispatchers.io) {
            try {
                val manifest = manifest ?: throw NoManifestError()
                val account = account ?: throw IllegalArgumentException("Empty account")

                val res = tonConnectKit.connect(
                    requestEntity,
                    manifest,
                    account.id,
                    account.toTonWalletFullAccess(
                        hardwarePublicKeyStorage,
                        BlockchainType.Ton,
                    )
                )
                println("TonConnect connect result: $res")
                finish = true
            } catch (e: Throwable) {
                toast = e.message?.nullIfBlank() ?: e.javaClass.simpleName
            }

            emitState()
        }
    }

    fun reject() {
        finish = true
        emitState()
    }

    fun onToastShow() {
        toast = null
        emitState()
    }
}

sealed class TonConnectError : Error()
class NoManifestError(override val message: String? = null) : TonConnectError()
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
