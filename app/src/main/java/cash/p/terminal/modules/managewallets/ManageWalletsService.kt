package cash.p.terminal.modules.managewallets

import cash.p.terminal.core.*
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.managers.RestoreSettings
import cash.p.terminal.core.managers.RestoreSettingsManager
import cash.p.terminal.entities.Account
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.ConfiguredToken
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.enablecoin.EnableCoinServiceXxx
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class ManageWalletsService(
    private val marketKit: MarketKitWrapper,
    private val walletManager: IWalletManager,
    accountManager: IAccountManager,
    private val enableCoinService: EnableCoinServiceXxx,
    private val restoreSettingsManager: RestoreSettingsManager,
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

        enableCoinService.enableCoinObservable
            .subscribeIO { (configuredPlatformCoins, settings) ->
                handleEnableCoin(configuredPlatformCoins, settings)
            }.let {
                disposables.add(it)
            }

        enableCoinService.enableSingleCoinObservable
            .subscribeIO { (configuredPlatformCoins, settings) ->
                handleEnableSingleCoin(configuredPlatformCoins, settings)
            }.let {
                disposables.add(it)
            }

        sync(walletManager.activeWallets)
        syncFullCoins()
        sortFullCoins()
        syncState()
    }

    private fun isEnabled(token: Token): Boolean {
        return wallets.any { it.token == token }
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
        // todo
//        fullCoins = fullCoins.sortedByFilter(filter) {
//            isEnabled(it.coin)
//        }
    }

    private fun item(fullCoin: FullCoin): List<Item> {
        val accountType = account?.type ?: return listOf()

        return fullCoin.eligibleTokens(accountType).map { token ->
            Item(
                token = token,
                enabled = isEnabled(token),
                hasSettings = isEnabled(token) && hasSettingsOrPlatforms(
                    fullCoin.eligibleTokens(
                        accountType
                    )
                ),
                hasInfo = isEnabled(token) && fullCoin.tokens.firstOrNull()?.blockchainType == BlockchainType.Zcash
            )
        }
    }

    private fun hasSettingsOrPlatforms(tokens: List<Token>): Boolean {
        return if (tokens.size == 1) {
            val token = tokens[0]
            token.blockchainType.coinSettingType != null || token.type !is TokenType.Native
        } else {
            true
        }
    }

    private fun syncState() {
        items = fullCoins.map { item(it) }.flatten()
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

    private fun handleEnableCoin(
        configuredTokens: List<ConfiguredToken>, restoreSettings: RestoreSettings
    ) {
        val account = this.account ?: return

        if (restoreSettings.isNotEmpty() && configuredTokens.size == 1) {
            enableCoinService.save(restoreSettings, account, configuredTokens.first().token.blockchainType)
        }

        val newWallets = configuredTokens.map { Wallet(it, account) }

        if (newWallets.isNotEmpty()) {
            walletManager.save(newWallets)
        }
    }

    private fun handleEnableSingleCoin(
        configuredToken: ConfiguredToken, restoreSettings: RestoreSettings
    ) {
        val account = this.account ?: return

        if (restoreSettings.isNotEmpty()) {
            enableCoinService.save(restoreSettings, account, configuredToken.token.blockchainType)
        }

        walletManager.save(listOf(Wallet(configuredToken, account)))
    }

    fun setFilter(filter: String) {
        this.filter = filter

        syncFullCoins()
        sortFullCoins()
        syncState()
    }

    fun enable(token: Token) {
        val account = this.account ?: return
        enableCoinService.enable(token, account.type, account)
    }

    fun disable(token: Token) {
        wallets.firstOrNull { it.token == token }?.let {
            walletManager.delete(listOf(it))
        }
    }

    fun configure(uid: String) {
        val account = this.account ?: return
        val fullCoin = fullCoins.firstOrNull { it.coin.uid == uid } ?: return
        val coinWallets = wallets.filter { it.coin == fullCoin.coin }
        enableCoinService.configure(fullCoin, account.type, coinWallets.map { it.configuredToken })
    }

    fun birthdayHeight(uid: String): Pair<Blockchain, Long>? {
        val token = fullCoins.firstOrNull { it.coin.uid == uid }?.tokens?.firstOrNull() ?: return null
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
        val token: Token,
        val enabled: Boolean,
        val hasSettings: Boolean,
        val hasInfo: Boolean
    )
}
