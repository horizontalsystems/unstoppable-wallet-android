package io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.isDefault
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.core.managers.TokenAutoEnableManager
import io.horizontalsystems.bankwallet.core.nativeTokenQueries
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.core.restoreSettingTypes
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.core.supported
import io.horizontalsystems.bankwallet.core.supports
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.enablecoin.blockchaintokens.BlockchainTokensService
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class RestoreBlockchainsService(
    private val accountName: String,
    private val accountType: AccountType,
    private val manualBackup: Boolean,
    private val fileBackup: Boolean,
    private val accountFactory: IAccountFactory,
    private val accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val marketKit: MarketKitWrapper,
    private val tokenAutoEnableManager: TokenAutoEnableManager,
    private val blockchainTokensService: BlockchainTokensService,
    private val restoreSettingsService: RestoreSettingsService
) : Clearable {

    private val disposables = CompositeDisposable()

    private var tokens = listOf<Token>()
    private val enabledTokens = mutableListOf<Token>()

    private var restoreSettingsMap = mutableMapOf<Token, RestoreSettings>()

    val cancelEnableBlockchainObservable = PublishSubject.create<Blockchain>()
    val canRestore = BehaviorSubject.createDefault(false)

    val itemsObservable = BehaviorSubject.create<List<Item>>()
    var items: List<Item> = listOf()
        private set(value) {
            field = value
            itemsObservable.onNext(value)
        }

    init {
        blockchainTokensService.approveTokensObservable
            .subscribeIO {
                handleApproveTokens(it.blockchain, it.tokens)
            }
            .let { disposables.add(it) }

        blockchainTokensService.rejectApproveTokensObservable
            .subscribeIO {
                handleCancelEnable(it)
            }
            .let { disposables.add(it) }

        restoreSettingsService.approveSettingsObservable
            .subscribeIO {
                handleApproveRestoreSettings(it.token, it.settings)
            }
            .let { disposables.add(it) }

        restoreSettingsService.rejectApproveSettingsObservable
            .subscribeIO {
                handleCancelEnable(it.blockchain)
            }
            .let { disposables.add(it) }

        syncInternalItems()
        syncState()
    }

    private fun syncInternalItems() {
        val allowedBlockchainTypes = BlockchainType.supported.filter { it.supports(accountType) }
        val tokenQueries = allowedBlockchainTypes
            .map { it.nativeTokenQueries }
            .flatten()

        tokens = marketKit.tokens(tokenQueries)
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
            cancelEnableBlockchainObservable.onNext(blockchain)
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
        canRestore.onNext(enabledTokens.isNotEmpty())
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
        val account = accountFactory.account(accountName, accountType, AccountOrigin.Restored, manualBackup, fileBackup)
        accountManager.save(account)

        restoreSettingsMap.forEach { (token, settings) ->
            restoreSettingsService.save(settings, account, token.blockchainType)
        }

        items.filter { it.enabled }.forEach { item ->
            tokenAutoEnableManager.markAutoEnable(account, item.blockchain.type)
        }

        if (enabledTokens.isEmpty()) return

        val wallets = enabledTokens.map { Wallet(it, account) }
        walletManager.save(wallets)
    }

    override fun clear() = disposables.clear()

    data class Item(
        val blockchain: Blockchain,
        val enabled: Boolean,
        val hasSettings: Boolean
    )

}
