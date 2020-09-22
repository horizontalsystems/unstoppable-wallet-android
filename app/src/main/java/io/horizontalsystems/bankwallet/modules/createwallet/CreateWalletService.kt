package io.horizontalsystems.bankwallet.modules.createwallet

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.reactivex.subjects.BehaviorSubject

class CreateWalletService(
        private val predefinedAccountType: PredefinedAccountType?,
        private val coinManager: ICoinManager,
        private val accountCreator: IAccountCreator,
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val derivationSettingsManager: IDerivationSettingsManager
) : CreateWalletModule.IService, Clearable {

    override val canCreate = BehaviorSubject.create<Boolean>()
    override var state: State = State()

    private val accounts = mutableMapOf<PredefinedAccountType, Account>()
    private val wallets = mutableMapOf<Coin, Wallet>()

    init {
        syncState()
    }

    override fun enable(coin: Coin) {
        val account = resolveAccount(coin.type.predefinedAccountType)
        wallets[coin] = Wallet(coin, account)

        syncState()
        syncCanCreate()
    }

    override fun disable(coin: Coin) {
        wallets.remove(coin)

        syncState()
        syncCanCreate()
    }

    override fun create() {
        if (wallets.isEmpty()){
            throw CreateError.NoWallets
        }

        val accounts = wallets.values.map { it.account }
        accounts.forEach {
            accountManager.save(it)
        }

        derivationSettingsManager.reset()

        walletManager.save(wallets.map { it.value })
    }

    override fun clear() {
    }

    private fun filteredCoins(coins: List<Coin>): List<Coin> {
        return predefinedAccountType?.let { type ->
            coins.filter { it.type.predefinedAccountType == type }
        } ?: coins
    }

    private fun item(coin: Coin): Item? {
        return if (coin.type.predefinedAccountType.isCreationSupported()) {
            Item(coin, wallets.containsKey(coin))
        } else {
            null
        }
    }

    private fun syncState() {
        val featuredCoins = filteredCoins(coinManager.featuredCoins)
        val coins = filteredCoins(coinManager.coins).filter { !featuredCoins.contains(it) }

        state = State(featuredCoins.mapNotNull { item(it) }, coins.mapNotNull { item(it) })
    }

    private fun syncCanCreate() {
        canCreate.onNext(wallets.isNotEmpty())
    }

    private fun resolveAccount(predefinedAccountType: PredefinedAccountType) : Account {
        accounts[predefinedAccountType]?.let {
            return it
        }

        val account = accountCreator.newAccount(predefinedAccountType)
        accounts[predefinedAccountType] = account
        return account
    }


    data class State(var featured: List<Item>, var items: List<Item>) {
        constructor() : this(listOf(), listOf())
    }

    data class Item(val coin: Coin, val enabled: Boolean)

    sealed class CreateError : Exception() {
        object NoWallets : CreateError()
    }
}
