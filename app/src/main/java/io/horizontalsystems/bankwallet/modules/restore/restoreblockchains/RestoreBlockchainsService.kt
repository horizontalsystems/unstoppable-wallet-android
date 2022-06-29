package io.horizontalsystems.bankwallet.modules.restore.restoreblockchains

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.ConfiguredToken
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.enablecoin.EnableCoinService
import io.horizontalsystems.bankwallet.modules.restore.restoreblockchains.RestoreBlockchainsModule.Blockchain
import io.horizontalsystems.bankwallet.modules.restore.restoreblockchains.RestoreBlockchainsModule.InternalItem
import io.horizontalsystems.xxxkit.MarketKit
import io.horizontalsystems.xxxkit.models.FullCoin
import io.horizontalsystems.xxxkit.models.Token
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class RestoreBlockchainsService(
    private val accountName: String,
    private val accountType: AccountType,
    private val accountFactory: IAccountFactory,
    private val accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val marketKit: MarketKit,
    private val enableCoinService: EnableCoinService,
    private val evmBlockchainManager: EvmBlockchainManager
) : Clearable {

    private val disposables = CompositeDisposable()

    private var internalItems = listOf<InternalItem>()
    private val enabledCoins = mutableListOf<ConfiguredToken>()

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
        val tokens = marketKit.tokens(Blockchain.all.map { it.tokenQuery })
        internalItems = Blockchain.all.mapNotNull { blockchain ->
            tokens.firstOrNull { it.tokenQuery == blockchain.tokenQuery }?.let { token ->
                InternalItem(blockchain, token)
            }
        }
    }

    private fun handleEnableCoin(
        configuredTokens: List<ConfiguredToken>,
        restoreSettings: RestoreSettings
    ) {
        val platformCoin = configuredTokens.firstOrNull()?.token ?: return

        if (restoreSettings.isNotEmpty()) {
            restoreSettingsMap[platformCoin] = restoreSettings
        }

        val existingConfiguredPlatformCoins = enabledCoins.filter { it.token == platformCoin }
        val newConfiguredPlatformCoins = configuredTokens.minus(existingConfiguredPlatformCoins)
        val removedConfiguredPlatformCoins = existingConfiguredPlatformCoins.minus(configuredTokens)

        enabledCoins.addAll(newConfiguredPlatformCoins)
        enabledCoins.removeAll(removedConfiguredPlatformCoins)

        syncCanRestore()
        syncState()
    }

    private fun handleCancelEnable(fullCoin: FullCoin) {
        val internalItem =
            internalItems.firstOrNull { fullCoin.supportedTokens.contains(it.token) }
                ?: return

        if (!isEnabled(internalItem)) {
            cancelEnableBlockchainObservable.onNext(internalItem.blockchain)
        }
    }

    private fun isEnabled(internalItem: InternalItem): Boolean {
        return enabledCoins.any { it.token == internalItem.token }
    }

    private fun item(internalItem: InternalItem): Item {
        val enabled = isEnabled(internalItem)
        val hasSettings = enabled && hasSettings(internalItem.token)
        return Item(internalItem.blockchain, enabled, hasSettings)
    }

    private fun hasSettings(token: Token) =
        token.blockchainType.coinSettingTypes.isNotEmpty()

    private fun syncState() {
        items = internalItems.map { item(it) }
    }

    private fun syncCanRestore() {
        canRestore.onNext(enabledCoins.isNotEmpty())
    }

    private fun getInternalItemByBlockchain(blockchain: Blockchain): InternalItem? =
        internalItems.firstOrNull { it.blockchain.uid == blockchain.uid }

    fun enable(blockchain: Blockchain) {
        val internalItem = getInternalItemByBlockchain(blockchain) ?: return

        enableCoinService.enable(internalItem.token.fullCoin)
    }

    fun disable(blockchain: Blockchain) {
        val internalItem = getInternalItemByBlockchain(blockchain) ?: return
        enabledCoins.removeIf { it.token == internalItem.token }

        syncState()
        syncCanRestore()
    }

    fun configure(blockchain: Blockchain) {
        val internalItem = getInternalItemByBlockchain(blockchain) ?: return

        enableCoinService.configure(
            internalItem.token.fullCoin,
            enabledCoins.filter { it.token == internalItem.token })
    }

    fun restore() {
        val account = accountFactory.account(accountName, accountType, AccountOrigin.Restored, true)
        accountManager.save(account)

        restoreSettingsMap.forEach { (token, settings) ->
            enableCoinService.save(settings, account, token.blockchainType)
        }

        items.filter { it.enabled }.forEach { item ->
            (item.blockchain as? Blockchain.Evm)?.let { evmBlockchain ->
                evmBlockchainManager.getEvmAccountManager(evmBlockchain.evmBlockchain.blockchainType).markAutoEnable(account)
            }
        }

        if (enabledCoins.isEmpty()) return

        val wallets = enabledCoins.map { Wallet(it, account) }
        walletManager.save(wallets)
    }

    override fun clear() = disposables.clear()

    data class Item(
        val blockchain: Blockchain,
        val enabled: Boolean,
        val hasSettings: Boolean
    )

}
