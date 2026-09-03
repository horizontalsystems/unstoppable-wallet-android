package io.horizontalsystems.walletkit.modules.restoreaccount.restoreblockchains

import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.IAccountFactory
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.isDefault
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.managers.TokenAutoEnableManager
import io.horizontalsystems.walletkit.core.managers.WalletManager
import io.horizontalsystems.walletkit.core.nativeTokenQueries
import io.horizontalsystems.walletkit.core.order
import io.horizontalsystems.walletkit.core.restoreSettingTypes
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.core.stats.statAccountType
import io.horizontalsystems.walletkit.core.supported
import io.horizontalsystems.walletkit.core.supports
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.enablecoin.blockchaintokens.BlockchainTokensService
import io.horizontalsystems.walletkit.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class RestoreBlockchainsService(
    private val accountName: String,
    private val accountType: AccountType,
    private val manualBackup: Boolean,
    private val fileBackup: Boolean,
    private val accountFactory: IAccountFactory,
    private val accountManager: IAccountManager,
    private val walletManager: WalletManager,
    private val marketKit: MarketKitWrapper,
    private val tokenAutoEnableManager: TokenAutoEnableManager,
    private val blockchainTokensService: BlockchainTokensService,
    private val restoreSettingsService: RestoreSettingsService,
    private val statPage: StatPage
) : Clearable {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var tokens = listOf<Token>()
    private val enabledTokens = CopyOnWriteArrayList<Token>()

    private var restoreSettingsMap = mutableMapOf<Token, RestoreSettings>()

    private val _cancelEnableBlockchainFlow = MutableSharedFlow<Blockchain>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val cancelEnableBlockchainFlow: Flow<Blockchain> = _cancelEnableBlockchainFlow

    private val _canRestore = MutableStateFlow(false)
    val canRestore: StateFlow<Boolean> = _canRestore

    private val _itemsFlow = MutableSharedFlow<List<Item>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val itemsFlow: Flow<List<Item>> = _itemsFlow
    var items: List<Item> = listOf()
        private set(value) {
            field = value
            itemsFlow.tryEmit(value)
        }

    init {
        coroutineScope.launch {
            blockchainTokensService.approveTokensFlow.collect {
                handleApproveTokens(it.blockchain, it.tokens)
            }
        }
        coroutineScope.launch {
            blockchainTokensService.rejectApproveTokensFlow.collect {
                handleCancelEnable(it)
            }
        }
        coroutineScope.launch {
            restoreSettingsService.approveSettingsFlow.collect {
                handleApproveRestoreSettings(it.token, it.settings)
            }
        }
        coroutineScope.launch {
            restoreSettingsService.rejectApproveSettingsFlow.collect {
                handleCancelEnable(it.blockchain)
            }
        }

        syncInternalItems()
        syncState()
    }

    private fun syncInternalItems() {
        val allowedBlockchainTypes = BlockchainType.supported.filter { it.supports(accountType) }
        val tokenQueries = allowedBlockchainTypes
            .map { it.nativeTokenQueries }
            .flatten()

        tokens = marketKit.tokens(tokenQueries)
            .filter { it.supports(accountType) }
            .sortedBy { it.type.order }
    }

    private fun handleApproveTokens(blockchain: Blockchain, tokens: List<Token>) {
        val existingTokens = enabledTokens.filter { it.blockchain == blockchain }

        val newTokens = tokens.minus(existingTokens)
        val removedTokens = existingTokens.minus(tokens)

        enabledTokens.addAll(newTokens)
        enabledTokens.removeAll(removedTokens)

        syncCanRestore()
        syncState()
    }
    private fun handleApproveRestoreSettings(
        token: Token,
        restoreSettings: RestoreSettings
    ) {
        if (restoreSettings.isNotEmpty()) {
            restoreSettingsMap[token] = restoreSettings
        }

        enabledTokens.add(token)

        syncCanRestore()
        syncState()
    }

    private fun handleCancelEnable(blockchain: Blockchain) {
        if (!isEnabled(blockchain)) {
            cancelEnableBlockchainFlow.tryEmit(blockchain)
        }
    }

    private fun isEnabled(blockchain: Blockchain): Boolean {
        return enabledTokens.any { it.blockchain == blockchain }
    }

    private fun item(blockchain: Blockchain): Item {
        val enabled = isEnabled(blockchain)
        val hasSettings = enabled && hasSettings(blockchain)
        return Item(blockchain, enabled, hasSettings)
    }

    private fun hasSettings(blockchain: Blockchain): Boolean {
        return tokens.count { it.blockchain == blockchain } > 1
    }

    private fun syncState() {
        val blockchains = tokens.map { it.blockchain }.toSet()
        items = blockchains.sortedBy { it.type.order }.map { item(it) }
    }

    private fun syncCanRestore() {
        canRestore.tryEmit(enabledTokens.isNotEmpty())
    }

    fun enable(blockchain: Blockchain) {
        val tokens = tokens.filter { it.blockchain == blockchain }
        val token = tokens.firstOrNull() ?: return

        if (tokens.size == 1) {
            if (token.blockchainType.restoreSettingTypes.isNotEmpty()) {
                restoreSettingsService.approveSettings(token)
            } else {
                handleApproveRestoreSettings(token, RestoreSettings())
            }
        } else {
            blockchainTokensService.approveTokens(blockchain, tokens, tokens.filter { it.type.isDefault })
        }
    }

    fun disable(blockchain: Blockchain) {
        enabledTokens.removeIf { it.blockchain == blockchain }

        syncState()
        syncCanRestore()
    }

    fun configure(blockchain: Blockchain) {
        val tokens = tokens.filter { it.blockchain == blockchain }
        if (tokens.isEmpty()) return

        val enabledTokens = enabledTokens.filter { it.blockchain == blockchain }

        blockchainTokensService.approveTokens(blockchain, tokens, enabledTokens, true)
    }

    fun restore() {
        val account = accountFactory.account(
            accountName,
            accountType,
            AccountOrigin.Restored,
            manualBackup,
            fileBackup,
        )
        accountManager.save(account)

        restoreSettingsMap.forEach { (token, settings) ->
            // reload = false: wallets for this restored account are enabled right
            // after; a reload here would spuriously churn their fresh adapters.
            restoreSettingsService.save(settings, account, token.blockchainType, reload = false)
        }

        items.filter { it.enabled }.forEach { item ->
            tokenAutoEnableManager.markAutoEnable(account, item.blockchain.type)
        }

        if (enabledTokens.isEmpty()) return

        val wallets = enabledTokens.map { Wallet(it, account) }
        walletManager.save(wallets)

        stat(page = statPage, event = StatEvent.ImportWallet(accountType.statAccountType))
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    data class Item(
        val blockchain: Blockchain,
        val enabled: Boolean,
        val hasSettings: Boolean
    )

}
