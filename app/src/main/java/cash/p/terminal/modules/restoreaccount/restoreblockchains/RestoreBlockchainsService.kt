package cash.p.terminal.modules.restoreaccount.restoreblockchains

import cash.p.terminal.core.Clearable
import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.IWalletManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.managers.RestoreSettings
import cash.p.terminal.core.managers.TokenAutoEnableManager
import cash.p.terminal.core.nativeTokenQueries
import cash.p.terminal.core.order
import cash.p.terminal.core.restoreSettingTypes
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.core.supportedTokens
import cash.p.terminal.core.supports
import cash.p.terminal.entities.AccountOrigin
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.enablecoin.coinplatforms.CoinTokensService
import cash.p.terminal.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
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
    private val evmBlockchainManager: EvmBlockchainManager,
    private val tokenAutoEnableManager: TokenAutoEnableManager,
    private val coinTokensService: CoinTokensService,
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

    private val blockchainTypes = listOf(
        BlockchainType.Bitcoin,
        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.ArbitrumOne,
        BlockchainType.Gnosis,
        BlockchainType.Fantom,
        BlockchainType.Zcash,
        BlockchainType.Dash,
        BlockchainType.BitcoinCash,
        BlockchainType.Litecoin,
        BlockchainType.BinanceChain,
        BlockchainType.Solana,
        BlockchainType.ECash,
        BlockchainType.Tron,
    )

    init {
        coinTokensService.approveTokensObservable
            .subscribeIO {
                handleEnableCoin(it.tokens, RestoreSettings())
            }
            .let { disposables.add(it) }

        coinTokensService.rejectApproveTokensObservable
            .subscribeIO {
                handleCancelEnable(it)
            }
            .let { disposables.add(it) }

        restoreSettingsService.approveSettingsObservable
            .subscribeIO {
                handleEnableCoin(listOf(it.token), it.settings)
            }
            .let { disposables.add(it) }

        restoreSettingsService.rejectApproveSettingsObservable
            .subscribeIO {
                handleCancelEnable(it.fullCoin)
            }
            .let { disposables.add(it) }

        syncInternalItems()
        syncState()
    }

    private fun syncInternalItems() {
        val allowedBlockchainTypes = blockchainTypes.filter { it.supports(accountType) }
        val tokenQueries = allowedBlockchainTypes
                .map { it.nativeTokenQueries }
                .flatten()

        tokens = marketKit.tokens(tokenQueries)
    }

    private fun handleEnableCoin(
        tokens: List<Token>,
        restoreSettings: RestoreSettings
    ) {
        val platformCoin = tokens.firstOrNull() ?: return

        if (restoreSettings.isNotEmpty()) {
            restoreSettingsMap[platformCoin] = restoreSettings
        }

        val existingTokens = enabledTokens.filter { it == platformCoin }
        val newTokens = tokens.minus(existingTokens)
        val removedTokens = existingTokens.minus(tokens)

        enabledTokens.addAll(newTokens)
        enabledTokens.removeAll(removedTokens)

        syncCanRestore()
        syncState()
    }

    private fun handleCancelEnable(fullCoin: FullCoin) {
        val token = tokens.firstOrNull { it.coin == fullCoin.coin } ?: return

        if (!isEnabled(token)) {
            cancelEnableBlockchainObservable.onNext(token.blockchain)
        }
    }

    private fun isEnabled(token: Token): Boolean {
        return enabledTokens.any { it == token }
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
        val fullCoin = FullCoin(token.coin, tokens)

        val supportedTokens = fullCoin.supportedTokens
        if (supportedTokens.size == 1) {
            val supportedToken = supportedTokens.first()
            when {
                supportedToken.blockchainType.restoreSettingTypes.isNotEmpty() -> {
                    restoreSettingsService.approveSettings(supportedToken)
                }

                supportedToken.type != TokenType.Native -> {
                    coinTokensService.approveTokens(fullCoin)
                }

                else -> {
                    handleEnableCoin(listOf(supportedToken), RestoreSettings())
                }
            }
        } else {
            coinTokensService.approveTokens(fullCoin)
        }
    }

    fun disable(blockchain: Blockchain) {
        enabledTokens.removeIf { it.blockchain == blockchain }

        syncState()
        syncCanRestore()
    }

    fun configure(blockchain: Blockchain) {
        val tokens = tokens.filter { it.blockchain == blockchain }
        val enabledTokens = enabledTokens.filter { it.blockchain == blockchain }
        val token = tokens.firstOrNull() ?: return

        coinTokensService.approveTokens(FullCoin(token.coin, tokens), enabledTokens, true)
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
