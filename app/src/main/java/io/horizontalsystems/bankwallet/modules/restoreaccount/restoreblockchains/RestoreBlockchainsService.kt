package io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.core.managers.TokenAutoEnableManager
import io.horizontalsystems.bankwallet.core.nativeTokenQueries
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.core.supports
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.ConfiguredToken
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.enablecoin.EnableCoinService
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
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
    private val enableCoinService: EnableCoinService,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val tokenAutoEnableManager: TokenAutoEnableManager
) : Clearable {

    private val disposables = CompositeDisposable()

    private var tokens = listOf<Token>()
    private val enabledTokens = mutableListOf<ConfiguredToken>()

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
        enableCoinService.enableCoinObservable
            .subscribeIO { (configuredPlatformCoins, settings) ->
                handleEnableCoin(configuredPlatformCoins, settings)
            }.let {
                disposables.add(it)
            }

        enableCoinService.cancelEnableCoinObservable
            .subscribeIO { fullCoin ->
                handleCancelEnable(fullCoin)
            }.let { disposables.add(it) }

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
        configuredTokens: List<ConfiguredToken>,
        restoreSettings: RestoreSettings
    ) {
        val platformCoin = configuredTokens.firstOrNull()?.token ?: return

        if (restoreSettings.isNotEmpty()) {
            restoreSettingsMap[platformCoin] = restoreSettings
        }

        val existingConfiguredPlatformCoins = enabledTokens.filter { it.token == platformCoin }
        val newConfiguredPlatformCoins = configuredTokens.minus(existingConfiguredPlatformCoins)
        val removedConfiguredPlatformCoins = existingConfiguredPlatformCoins.minus(configuredTokens)

        enabledTokens.addAll(newConfiguredPlatformCoins)
        enabledTokens.removeAll(removedConfiguredPlatformCoins)

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
        return enabledTokens.any { it.token == token }
    }

    private fun isEnabled(blockchain: Blockchain): Boolean {
        return enabledTokens.any { it.token.blockchain == blockchain }
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
        enableCoinService.enable(FullCoin(token.coin, tokens), accountType)
    }

    fun disable(blockchain: Blockchain) {
        enabledTokens.removeIf { it.token.blockchain == blockchain }

        syncState()
        syncCanRestore()
    }

    fun configure(blockchain: Blockchain) {
        val tokens = tokens.filter { it.blockchain == blockchain }
        val enabledTokens = enabledTokens.filter { it.token.blockchain == blockchain }
        val token = tokens.firstOrNull() ?: return

        enableCoinService.configure(FullCoin(token.coin, tokens), accountType, enabledTokens)
    }

    fun restore() {
        val account = accountFactory.account(accountName, accountType, AccountOrigin.Restored, manualBackup, fileBackup)
        accountManager.save(account)

        restoreSettingsMap.forEach { (token, settings) ->
            enableCoinService.save(settings, account, token.blockchainType)
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
