package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.enablecoin.EnableCoinService
import io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins.RestoreSelectCoinsModule.Blockchain
import io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins.RestoreSelectCoinsModule.InternalItem
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class RestoreSelectCoinsService(
    private val accountType: AccountType,
    private val accountFactory: IAccountFactory,
    private val accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val coinManager: ICoinManager,
    private val enableCoinService: EnableCoinService
) : Clearable {

    private val disposables = CompositeDisposable()

    private var internalItems = listOf<InternalItem>()
    val enabledCoins = mutableListOf<ConfiguredPlatformCoin>()

    private var restoreSettingsMap = mutableMapOf<PlatformCoin, RestoreSettings>()
    private var enableCoinServiceIsBusy = false

    val cancelEnableBlockchainObservable = PublishSubject.create<Blockchain>()
    val canRestore = BehaviorSubject.createDefault(false)

    val itemsObservable = BehaviorSubject.create<List<Item>>()
    var items: List<Item> = listOf()
        set(value) {
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
        val platformCoins = coinManager.getPlatformCoins(Blockchain.values().map { it.coinType })
        internalItems = Blockchain.values().mapNotNull { blockchain ->
            platformCoins.firstOrNull { it.coinType == blockchain.coinType }?.let { platformCoin ->
                InternalItem(blockchain, platformCoin)
            }
        }
    }

    private fun handleEnableCoin(
        configuredPlatformCoins: List<ConfiguredPlatformCoin>,
        restoreSettings: RestoreSettings
    ) {
        val platformCoin = configuredPlatformCoins.firstOrNull()?.platformCoin ?: return

        if (restoreSettings.isNotEmpty()) {
            restoreSettingsMap[platformCoin] = restoreSettings
        }

        val existingConfiguredPlatformCoins =
            enabledCoins.filter { it.platformCoin == platformCoin }
        val newConfiguredPlatformCoins =
            configuredPlatformCoins.minus(existingConfiguredPlatformCoins)
        val removedConfiguredPlatformCoins =
            existingConfiguredPlatformCoins.minus(configuredPlatformCoins)

        enabledCoins.addAll(newConfiguredPlatformCoins)
        enabledCoins.removeAll(removedConfiguredPlatformCoins)

        syncCanRestore()
        syncState()
    }

    private fun handleCancelEnable(fullCoin: FullCoin) {
        val internalItem =
            internalItems.firstOrNull { fullCoin.platforms.contains(it.platformCoin.platform) }
                ?: return

        if (!isEnabled(internalItem)) {
            cancelEnableBlockchainObservable.onNext(internalItem.blockchain)
        }
    }

    private fun isEnabled(internalItem: InternalItem): Boolean {
        return enabledCoins.any { it.platformCoin == internalItem.platformCoin }
    }

    private fun item(internalItem: InternalItem): Item {
        val enabled = isEnabled(internalItem)
        val state = ItemState.Supported(
            enabled,
            hasSettings = enabled && hasSettings(internalItem.platformCoin)
        )
        return Item(internalItem.blockchain, state)
    }

    private fun hasSettings(platformCoin: PlatformCoin) =
        platformCoin.coinType.coinSettingTypes.isNotEmpty()

    private fun syncState() {
        items = internalItems.map { item(it) }
    }

    private fun syncCanRestore() {
        canRestore.onNext(enabledCoins.isNotEmpty() && !enableCoinServiceIsBusy)
    }

    fun enable(blockchain: Blockchain) {
        val internalItem = internalItems.firstOrNull { it.blockchain == blockchain } ?: return

        enableCoinService.enable(internalItem.platformCoin.fullCoin)
    }

    fun disable(blockchain: Blockchain) {
        val internalItem = internalItems.firstOrNull { it.blockchain == blockchain } ?: return
        enabledCoins.removeIf { it.platformCoin == internalItem.platformCoin }

        syncState()
        syncCanRestore()
    }

    fun configure(blockchain: Blockchain) {
        val internalItem = internalItems.firstOrNull { it.blockchain == blockchain } ?: return

        enableCoinService.configure(
            internalItem.platformCoin.fullCoin,
            enabledCoins.filter { it.platformCoin == internalItem.platformCoin })
    }

    fun restore() {
        val account = accountFactory.account(accountType, AccountOrigin.Restored, true)
        accountManager.save(account)

        restoreSettingsMap.forEach { (platformCoin, settings) ->
            enableCoinService.save(settings, account, platformCoin.coinType)
        }

        if (enabledCoins.isEmpty()) return

        val wallets = enabledCoins.map { Wallet(it, account) }
        walletManager.save(wallets)
    }

    override fun clear() = disposables.clear()

    data class Item(
        val blockchain: Blockchain,
        val state: ItemState
    )

    sealed class ItemState {
        object Unsupported : ItemState()
        class Supported(val enabled: Boolean, val hasSettings: Boolean) : ItemState()
    }
}
