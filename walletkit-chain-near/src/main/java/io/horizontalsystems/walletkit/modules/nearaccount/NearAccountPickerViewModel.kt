package io.horizontalsystems.walletkit.modules.nearaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.nearkit.NearKit
import io.horizontalsystems.nearkit.crypto.PublicKey
import io.horizontalsystems.nearkit.network.Network
import io.horizontalsystems.walletkit.chain.near.NearChainPlugin
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.managers.RestoreSettingType
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.modules.enablecoin.restoresettings.AccountPickerResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URL

class NearAccountPickerViewModel(
    private val accountType: AccountType.Mnemonic,
    private val rpcUrls: List<URL>,
    private val fastNearApiKey: String?,
) : ViewModelUiState<NearAccountPickerUiState>() {

    private var publicKey: PublicKey? = null
    private var implicitAccountId: String? = null
    private var accounts = listOf<Account>()
    private var selectedAccountId: String? = null
    private var searching = true
    private var searchFailed = false
    private var result: AccountPickerResult? = null

    init {
        search()
    }

    override fun createState() = NearAccountPickerUiState(
        searching = searching,
        searchFailed = searchFailed,
        items = accounts.map { account ->
            NearAccountViewItem(
                accountId = account.accountId,
                implicit = account.accountId == implicitAccountId,
                balance = account.available?.let {
                    App.numberFormatter.formatCoinFull(BigDecimal(it, NEAR_DECIMALS), "NEAR", 8)
                },
                selected = account.accountId == selectedAccountId,
                accessLost = account.accessLost == true,
                otherFullAccessKeys = account.otherFullAccessKeys == true,
                hasContract = account.hasContract == true,
            )
        },
        publicKey = publicKey?.toString(),
        result = result,
    )

    fun retry() = search()

    fun onSelect(accountId: String) {
        selectedAccountId = accountId
        emitState()
    }

    /** An account the user entered by name; the name page has already checked the key's full access. */
    fun onAccountEntered(accountId: String) {
        if (accounts.none { it.accountId == accountId }) {
            accounts = accounts + Account(accountId)
            viewModelScope.launch {
                val account = loadAccount(accountId)
                accounts = accounts.map { if (it.accountId == accountId) account else it }
                emitState()
            }
        }
        searchFailed = false
        selectedAccountId = accountId
        emitState()
    }

    fun onContinue() {
        val accountId = selectedAccountId ?: return
        finish(settings(accountId))
    }

    fun onUseDefault() {
        finish(RestoreSettings())
    }

    fun onCancel() {
        result = AccountPickerResult(null)
        emitState()
    }

    private fun search() {
        searching = true
        searchFailed = false
        emitState()

        viewModelScope.launch {
            try {
                val publicKey = publicKey ?: withContext(Dispatchers.Default) {
                    NearKit.publicKey(accountType.seed)
                }.also {
                    publicKey = it
                    implicitAccountId = it.implicitAccountId
                }

                val accountIds = NearKit.findAccounts(publicKey, Network.MainNet, fastNearApiKey, rpcUrls)

                // Only the implicit account: nothing to choose, so the picker is never seen
                if (accountIds.size <= 1) {
                    finish(RestoreSettings())
                    return@launch
                }

                accounts = accountIds
                    .map { accountId -> async { loadAccount(accountId) } }
                    .awaitAll()
                selectedAccountId = preselected(accounts)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                searchFailed = true
            } finally {
                searching = false
                emitState()
            }
        }
    }

    // Details only inform the choice, so an account whose details fail to load is still listed
    private suspend fun loadAccount(accountId: String): Account = coroutineScope {
        val state = async { orNull { NearKit.accountState(accountId, Network.MainNet, fastNearApiKey, rpcUrls) } }
        val keys = async { orNull { NearKit.accessKeys(accountId, Network.MainNet, fastNearApiKey, rpcUrls) } }
        val ownKey = publicKey?.toString()
        val exists = state.await()?.exists
        val fullAccessKeys = keys.await()?.filter { it.isFullAccess }?.map { it.publicKey }
        Account(
            accountId = accountId,
            available = state.await()?.available,
            hasContract = state.await()?.hasContract,
            accessLost = if (exists == null || fullAccessKeys == null) null else exists && ownKey !in fullAccessKeys,
            otherFullAccessKeys = fullAccessKeys?.any { it != ownKey },
        )
    }

    private suspend fun <T> orNull(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        null
    }

    // Users moving from MyNearWallet keep their funds on a named account while the implicit one
    // of their new phrase is empty, so the richest account is the likely choice; on a tie, named.
    private fun preselected(accounts: List<Account>): String? =
        accounts.filter { it.accessLost != true }.maxWithOrNull(
            compareBy<Account> { it.available ?: BigInteger.ZERO }
                .thenBy { it.accountId != implicitAccountId }
        )?.accountId

    private fun settings(accountId: String) = RestoreSettings().apply {
        this[RestoreSettingType.NearAccountId] = accountId
    }

    private fun finish(settings: RestoreSettings) {
        result = AccountPickerResult(settings)
        emitState()
    }

    private data class Account(
        val accountId: String,
        val available: BigInteger? = null,
        val hasContract: Boolean? = null,
        /**
         * The account exists but the phrase's key is gone from it. Only the implicit account can
         * get here: the search lists named accounts only while the key has full access to them.
         */
        val accessLost: Boolean? = null,
        /** Whether a key other than the phrase's can also sign anything for the account. */
        val otherFullAccessKeys: Boolean? = null,
    )

    class Factory(private val accountType: AccountType.Mnemonic) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val plugin = requireNotNull(ChainRegistry[BlockchainType.Near] as? NearChainPlugin) {
                "NEAR plugin is not registered"
            }
            return NearAccountPickerViewModel(
                accountType,
                plugin.rpcSourceManager.rpcUrls(),
                App.appConfigProvider.fastNearApiKey,
            ) as T
        }
    }

    companion object {
        const val NEAR_DECIMALS = 24
    }
}

data class NearAccountPickerUiState(
    val searching: Boolean,
    val searchFailed: Boolean,
    val items: List<NearAccountViewItem>,
    val publicKey: String?,
    val result: AccountPickerResult?,
) {
    val continueEnabled: Boolean get() = items.any { it.selected }
    val selectedItem: NearAccountViewItem? get() = items.firstOrNull { it.selected }
}

data class NearAccountViewItem(
    val accountId: String,
    val implicit: Boolean,
    val balance: String?,
    val selected: Boolean,
    val accessLost: Boolean,
    val otherFullAccessKeys: Boolean,
    val hasContract: Boolean,
)
