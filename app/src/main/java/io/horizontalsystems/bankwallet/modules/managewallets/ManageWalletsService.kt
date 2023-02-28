package io.horizontalsystems.bankwallet.modules.managewallets

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class ManageWalletsService(
    private val marketKit: MarketKitWrapper,
    private val walletManager: IWalletManager,
    accountManager: IAccountManager,
    private val restoreSettingsManager: RestoreSettingsManager,
    private val restoreSettingsService: RestoreSettingsService,
) : Clearable {

    val itemsObservable = PublishSubject.create<List<Item>>()
    var items: List<Item> = listOf()
        private set(value) {
            field = value
            itemsObservable.onNext(value)
        }

    val accountType: AccountType?
        get() = account?.type

    private val account: Account? = accountManager.activeAccount
    private var wallets = setOf<Wallet>()
    private var fullCoins = listOf<FullCoin>()

    private val disposables = CompositeDisposable()

    private var filter: String = ""

    init {
        walletManager.activeWalletsUpdatedObservable
            .subscribeIO {
                handleUpdated(it)
            }
            .let {
                disposables.add(it)
            }

        restoreSettingsService.approveSettingsObservable
            .subscribeIO {
                enable(ConfiguredToken(it.token), it.settings)
            }.let {
                disposables.add(it)
            }

        sync(walletManager.activeWallets)
        syncFullCoins()
        sortFullCoins()
        syncState()
    }

    private fun isEnabled(configuredToken: ConfiguredToken): Boolean {
        return wallets.any { it.configuredToken == configuredToken }
    }

    private fun sync(walletList: List<Wallet>) {
        wallets = walletList.toSet()
    }

    private fun fetchFullCoins(): List<FullCoin> {
        return if (filter.isBlank()) {
            val account = this.account ?: return emptyList()
            val featuredFullCoins = marketKit.fullCoins("", 100).toMutableList()
                .filter { it.eligibleTokens(account.type).isNotEmpty() }

            val featuredCoins = featuredFullCoins.map { it.coin }
            val enabledFullCoins = marketKit.fullCoins(
                coinUids = wallets.filter { !featuredCoins.contains(it.coin) }.map { it.coin.uid }
            )
            val customFullCoins = wallets.filter { it.token.isCustom }.map { it.token.fullCoin }

            featuredFullCoins + enabledFullCoins + customFullCoins
        } else if (isContractAddress(filter)) {
            val tokens = marketKit.tokens(filter)
            val coinUids = tokens.map { it.coin.uid }
            marketKit.fullCoins(coinUids)
        } else {
            marketKit.fullCoins(filter, 20)
        }
    }

    private fun isContractAddress(filter: String) = try {
        AddressValidator.validate(filter)
        true
    } catch (e: AddressValidator.AddressValidationException) {
        false
    }

    private fun syncFullCoins() {
        fullCoins = fetchFullCoins()
    }

    private fun sortFullCoins() {
        fullCoins = fullCoins.sortedByFilter(filter)
    }

    private fun getItemsForFullCoin(fullCoin: FullCoin): List<Item> {
        val accountType = account?.type ?: return listOf()

        val items = mutableListOf<Item>()
        fullCoin.eligibleTokens(accountType).forEach { token ->
            items.addAll(getItemsForToken(token, accountType))
        }

        return items
    }

    private fun getItemsForToken(token: Token, accountType: AccountType): List<Item> {
        val items = mutableListOf<Item>()
        when (token.blockchainType.coinSettingType) {
            CoinSettingType.derivation -> {
                accountType.supportedDerivations.forEach {
                    val coinSettings = CoinSettings(mapOf(CoinSettingType.derivation to it.value))
                    items.add(getItemForConfiguredToken(ConfiguredToken(token, coinSettings)))
                }
            }
            CoinSettingType.bitcoinCashCoinType -> {
                BitcoinCashCoinType.values().forEach {
                    val coinSettings = CoinSettings(mapOf(CoinSettingType.bitcoinCashCoinType to it.value))
                    items.add(getItemForConfiguredToken(ConfiguredToken(token, coinSettings)))
                }
            }
            else -> {
                items.add(getItemForConfiguredToken(ConfiguredToken(token)))
            }
        }

        return items
    }

    private fun getItemForConfiguredToken(configuredToken: ConfiguredToken): Item {
        val enabled = isEnabled(configuredToken)

        val hasInfo = when (configuredToken.token.type) {
            is TokenType.Eip20,
            is TokenType.Bep2,
            is TokenType.Spl -> true
            is TokenType.Native -> when (configuredToken.token.blockchainType) {
                is BlockchainType.Bitcoin,
                is BlockchainType.Litecoin,
                is BlockchainType.BitcoinCash -> true
                is BlockchainType.Zcash -> enabled
                else -> false
            }
            else -> false
        }

        return Item(
            configuredToken = configuredToken,
            enabled = enabled,
            hasInfo = hasInfo
        )
    }

    private fun syncState() {
        items = fullCoins.map { getItemsForFullCoin(it) }.flatten()
    }

    private fun handleUpdated(wallets: List<Wallet>) {
        sync(wallets)

        val newFullCons = fetchFullCoins()
        if (newFullCons.size > fullCoins.size) {
            fullCoins = newFullCons
            sortFullCoins()
        }

        syncState()
    }

    private fun enable(configuredToken: ConfiguredToken, restoreSettings: RestoreSettings) {
        val account = this.account ?: return

        if (restoreSettings.isNotEmpty()) {
            restoreSettingsService.save(restoreSettings, account, configuredToken.token.blockchainType)
        }

        walletManager.save(listOf(Wallet(configuredToken, account)))
    }

    fun setFilter(filter: String) {
        this.filter = filter

        syncFullCoins()
        sortFullCoins()
        syncState()
    }

    fun enable(configuredToken: ConfiguredToken) {
        val account = this.account ?: return

        if (configuredToken.token.blockchainType.restoreSettingTypes.isNotEmpty()) {
            restoreSettingsService.approveSettings(configuredToken.token, account)
        } else {
            enable(configuredToken, RestoreSettings())
        }
    }

    fun disable(configuredToken: ConfiguredToken) {
        wallets.firstOrNull { it.configuredToken == configuredToken }?.let {
            walletManager.delete(listOf(it))
        }
    }

    fun birthdayHeight(configuredToken: ConfiguredToken): Pair<Blockchain, Long>? {
        val token = configuredToken.token
        val account = this.account ?: return null
        val settings = restoreSettingsManager.settings(account, token.blockchainType)

        return settings.birthdayHeight?.let {
            Pair(token.blockchain, it)
        }
    }

    override fun clear() {
        disposables.clear()
    }

    data class Item(
        val configuredToken: ConfiguredToken,
        val enabled: Boolean,
        val hasInfo: Boolean
    )
}
