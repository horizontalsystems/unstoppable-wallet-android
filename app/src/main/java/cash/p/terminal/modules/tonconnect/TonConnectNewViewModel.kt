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
import io.horizontalsystems.tonkit.tonconnect.TonConnectKit
import kotlinx.coroutines.delay
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
    private var connecting = false
    private var finish = false
    private var error: Throwable? = null
    private var toast: String? = null

    override fun createState() = TonConnectNewUiState(
        manifest = manifest,
        accounts = accounts,
        account = account,
        connecting = connecting,
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
            val url = requestEntity.payload.manifestUrl
            for (attempt in 1..MAX_MANIFEST_RETRIES) {
                try {
                    manifest = tonConnectKit.getManifest(url)
                    if (error is NoManifestError) {
                        error = null
                    }
                    emitState()
                    return@launch
                } catch (e: Throwable) {
                    if (attempt < MAX_MANIFEST_RETRIES) {
                        delay(MANIFEST_RETRY_DELAY_MS)
                    } else {
                        error = NoManifestError(e.message)
                        emitState()
                    }
                }
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
        if (connecting) return

        connecting = true
        emitState()

        viewModelScope.launch(dispatchers.io) {
            try {
                val manifest = manifest ?: throw NoManifestError()
                val account = account ?: throw IllegalArgumentException("Empty account")

                tonConnectKit.connect(
                    requestEntity,
                    manifest,
                    account.id,
                    account.toTonWalletFullAccess(
                        hardwarePublicKeyStorage,
                        BlockchainType.Ton,
                    )
                )
                finish = true
            } catch (e: Throwable) {
                toast = e.message?.nullIfBlank() ?: e.javaClass.simpleName
                connecting = false
            }

            emitState()
        }
    }

    fun reject() {
        // No-op: navigation is handled by onResult callback.
        // Do NOT set finish=true here — it would trigger a second
        // popBackStack via LaunchedEffect(uiState.finish) on top of
        // the one in onResult, double-popping the back stack.
    }

    fun onToastShow() {
        toast = null
        emitState()
    }
}

private const val MAX_MANIFEST_RETRIES = 3
private const val MANIFEST_RETRY_DELAY_MS = 1000L

sealed class TonConnectError : Error()
class NoManifestError(override val message: String? = null) : TonConnectError()
class NoTonAccountError : TonConnectError()

data class TonConnectNewUiState(
    val manifest: DAppManifestEntity?,
    val accounts: List<Account>,
    val account: Account?,
    val connecting: Boolean,
    val finish: Boolean,
    val error: Throwable?,
    val toast: String?
) {
    val connectEnabled get() = error == null && account != null && !connecting
}
