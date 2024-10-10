package io.horizontalsystems.bankwallet.modules.tonconnect

import androidx.lifecycle.viewModelScope
import com.tonapps.wallet.data.tonconnect.entities.DAppManifestEntity
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.toTonKitWalletType
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TonConnectNewViewModel(
    private val requestEntity: DAppRequestEntity,
) : ViewModelUiState<TonConnectNewUiState>() {
    private val tonConnectKit = App.tonConnectKit

    private var manifest: DAppManifestEntity? = null
    private var accounts: List<Account> = listOf()
    private var account = App.accountManager.activeAccount
    private var finish = false

    override fun createState() = TonConnectNewUiState(
        manifest = manifest,
        accounts = accounts,
        account = account,
        finish = finish,
    )

    init {
        viewModelScope.launch(Dispatchers.Default) {
            manifest = tonConnectKit.getManifest(requestEntity.payload.manifestUrl)
            emitState()
        }

        accounts = App.accountManager.accounts.filter {
            it.type is AccountType.Mnemonic
        }
    }

    fun onSelectAccount(account: Account) {
        this.account = account
        emitState()
    }

    fun connect() {
        viewModelScope.launch {
            val manifest = manifest ?: throw IllegalArgumentException("Empty manifest")
            val account = account ?: throw IllegalArgumentException("Empty account")
            tonConnectKit.connect(requestEntity, manifest, account.type.toTonKitWalletType(), false)
            finish = true
            emitState()
        }
    }

    fun reject() {
        finish = true
        emitState()
    }
}

data class TonConnectNewUiState(
    val manifest: DAppManifestEntity?,
    val accounts: List<Account>,
    val account: Account?,
    val finish: Boolean
)
