package cash.p.terminal.modules.managewallets

import cash.p.terminal.core.eligibleTokens
import cash.p.terminal.core.managers.RestoreSettings
import cash.p.terminal.core.managers.UserDeletedWalletManager
import cash.p.terminal.core.restoreSettingTypes
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsService
import cash.p.terminal.modules.enablecoin.restoresettings.TokenConfig
import cash.p.terminal.modules.receive.FullCoinsProvider
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.Clearable
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.WalletFactory
import cash.p.terminal.wallet.entities.FullCoin
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.isLitecoinMweb
import cash.p.terminal.wallet.useCases.GetHardwarePublicKeyForWalletUseCase
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

class ManageWalletsService(
    private val walletManager: IWalletManager,
    private val restoreSettingsService: RestoreSettingsService,
    private val fullCoinsProvider: FullCoinsProvider?,
    private val account: Account?,
    private val userDeletedWalletManager: UserDeletedWalletManager
) : Clearable {

    private val getHardwarePublicKeyForWalletUseCase: GetHardwarePublicKeyForWalletUseCase by inject(
        GetHardwarePublicKeyForWalletUseCase::class.java
    )
    private val walletFactory: WalletFactory by inject(WalletFactory::class.java)
    private val marketKit: MarketKitWrapper by inject(MarketKitWrapper::class.java)

    private val _itemsFlow = MutableStateFlow<List<Item>>(listOf())
    val itemsFlow = _itemsFlow.asStateFlow()

    val accountType: AccountType?
        get() = account?.type

    private var fullCoins = listOf<FullCoin>()
    private var items = listOf<Item>()

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()

    private var filter: String = ""

    init {
        coroutineScope.launch {
            walletManager.activeWalletsFlow.collect {
                handleUpdated(it)
            }
        }
        coroutineScope.launch {
            restoreSettingsService.approveSettingsObservable.asFlow().collect {
                enable(it.token, it.settings)
            }
        }

        coroutineScope.launch {
            sync(walletManager.activeWallets)
            syncFullCoins()
            sortItems()
            syncState()
        }
    }

    private fun isEnabled(token: Token): Boolean {
        val contractAddress = (token.type as? TokenType.Eip20)?.address
        return if (contractAddress != null) {
            // For EIP20 tokens, check if any wallet with same contract exists
            walletManager.activeWallets.any {
                it.token.blockchainType == token.blockchainType &&
                    (it.token.type as? TokenType.Eip20)?.address == contractAddress
            }
        } else {
            walletManager.activeWallets.any { it.token == token }
        }
    }

    private fun sync(walletList: List<Wallet>) {
        fullCoinsProvider?.setActiveWallets(walletList)
    }

    private suspend fun fetchFullCoins(): List<FullCoin> = withContext(Dispatchers.IO) {
        fullCoinsProvider?.getItems() ?: listOf()
    }

    private suspend fun syncFullCoins() {
        mutex.withLock {
            fullCoins = fetchFullCoins()
        }
    }

    private suspend fun sortItems() {
        mutex.withLock {
            items = fullCoins
                .map { getItemsForFullCoin(it) }
                .flatten()
        }
    }

    private fun getItemsForFullCoin(fullCoin: FullCoin): List<Item> {
        val accountType = account?.type ?: return listOf()
        val eligibleTokens = fullCoin.eligibleTokens(accountType)
        return eligibleTokens.map { getItemForToken(it) }
    }

    private fun getItemForToken(token: Token): Item {
        val enabled = isEnabled(token)

        return Item(
            token = token,
            enabled = enabled,
            hasInfo = hasInfo(token, enabled)
        )
    }

    private fun hasInfo(token: Token, enabled: Boolean) = when (token.type) {
        is TokenType.Native -> token.blockchainType is BlockchainType.Zcash && enabled
        TokenType.Mweb,
        is TokenType.Derived,
        is TokenType.AddressTyped,
        is TokenType.Eip20,
        is TokenType.Spl,
        is TokenType.Jetton -> true

        else -> false
    }

    private fun syncState() {
        _itemsFlow.update {
            buildList { addAll(items) }
        }
    }

    private suspend fun handleUpdated(wallets: List<Wallet>) {
        sync(wallets)

        val newFullCons = fetchFullCoins()
        mutex.withLock {
            if (newFullCons.size > fullCoins.size) {
                fullCoins = newFullCons
                sortItems()
            }
        }
        syncState()
    }

    private suspend fun updateSortedItems(token: Token, enable: Boolean) {
        val relatedTokens = getTokensWithSameContract(token)
        mutex.withLock {
            items = items.map { item ->
                if (relatedTokens.any { it == item.token }) {
                    item.copy(
                        enabled = enable,
                        hasInfo = hasInfo(item.token, enable)
                    )
                } else {
                    item
                }
            }
        }
    }

    private suspend fun enable(token: Token, restoreSettings: RestoreSettings) {
        val account = this.account ?: return

        if (restoreSettings.isNotEmpty()) {
            restoreSettingsService.save(restoreSettings, account, token.blockchainType)
        }

        userDeletedWalletManager.unmarkAsDeleted(account.id, token.tokenQuery.id)

        val hardwarePublicKey = withContext(Dispatchers.IO) {
            getHardwarePublicKeyForWalletUseCase(account, token)
        }

        walletFactory.create(token, account, hardwarePublicKey)?.let {
            walletManager.save(listOf(it))
        }

        updateSortedItems(token, true)
        syncState()
    }

    fun setFilter(filter: String) {
        this.filter = filter
        fullCoinsProvider?.setQuery(filter)

        coroutineScope.launch {
            syncFullCoins()
            sortItems()
            syncState()
        }
    }

    fun enable(token: Token) {
        val account = this.account ?: return

        coroutineScope.launch {
            // Only create wallet for the clicked token (not all with same contract)
            // Toggle sync is handled by isEnabled() checking by contract address
            if (token.restoreSettingTypes.isNotEmpty()) {
                restoreSettingsService.approveSettings(
                    token = token,
                    account = account,
                    forceRequest = shouldForceBirthdayHeightDialog(token),
                    initialConfig = buildInitialConfig(token, account)
                )
            } else {
                enable(token, RestoreSettings())
            }
        }
    }

    fun disable(token: Token) {
        coroutineScope.launch {
            // Find and delete wallet with same contract address
            val contractAddress = (token.type as? TokenType.Eip20)?.address
            val walletToDelete = if (contractAddress != null) {
                walletManager.activeWallets.firstOrNull {
                    it.token.blockchainType == token.blockchainType &&
                        (it.token.type as? TokenType.Eip20)?.address == contractAddress
                }
            } else {
                walletManager.activeWallets.firstOrNull { it.token == token }
            }

            walletToDelete?.let {
                userDeletedWalletManager.markAsDeleted(it)
                walletManager.deleteByWallet(it)
                // Update UI state for all related tokens
                updateSortedItems(token, false)
                syncState()
            }
        }
    }

    /**
     * Find all tokens with the same contract address on the same blockchain.
     * Used for syncing toggles between virtual USDT and real BSC-USD.
     * Queries MarketKit directly to get ALL tokens with matching contract.
     */
    private fun getTokensWithSameContract(token: Token): List<Token> {
        val contractAddress = (token.type as? TokenType.Eip20)?.address ?: return listOf(token)

        // Query MarketKit for all tokens with this contract address
        return marketKit.tokens(contractAddress)
            .filter { it.blockchainType == token.blockchainType }
            .ifEmpty { listOf(token) }
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    data class Item(
        val token: Token,
        val enabled: Boolean,
        val hasInfo: Boolean
    )

    private fun shouldForceBirthdayHeightDialog(token: Token): Boolean {
        return when (token.blockchainType) {
            BlockchainType.Monero -> true
            BlockchainType.Zcash -> walletManager.activeWallets.none { wallet ->
                wallet.token.blockchainType == BlockchainType.Zcash
            }

            BlockchainType.Litecoin -> token.isLitecoinMweb
            else -> false
        }
    }

    private fun buildInitialConfig(token: Token, account: Account): TokenConfig? {
        val restoreSettings = restoreSettingsService.getSettings(account, token.blockchainType)
        val birthdayHeight = restoreSettings.birthdayHeight?.takeIf { it > 0 } ?: return null

        return TokenConfig(
            birthdayHeight = birthdayHeight.toString(),
            restoreAsNew = false
        )
    }
}
