package io.horizontalsystems.bankwallet.modules.createwallet

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.subjects.BehaviorSubject
import java.util.*

class CreateWalletService(
        private val predefinedAccountType: PredefinedAccountType?,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager,
        private val coinManager: ICoinManager,
        private val accountCreator: IAccountCreator,
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val derivationSettingsManager: IDerivationSettingsManager
) : CreateWalletModule.IService, Clearable {

    override val stateAsync = BehaviorSubject.create<State>()
    override val canCreateAsync = BehaviorSubject.create<Boolean>()
    override var state: State = State()
        set(value) {
            field = value
            stateAsync.onNext(value)
        }

    private var featuredCoins = listOf<Coin>()
    private var coins = listOf<Coin>()
    private val accounts = mutableMapOf<PredefinedAccountType, Account>()
    private val wallets = mutableMapOf<Coin, Wallet>()

    init {
        syncCoins()
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

        for (account in accounts) {
            if(predefinedAccountTypeManager.predefinedAccountType(account.type) == PredefinedAccountType.Standard){
                derivationSettingsManager.resetStandardSettings()
                break
            }
        }

        walletManager.save(wallets.map { it.value })
    }

    override fun clear() {
    }

    private fun syncCoins() {
        val (featuredCoins, regularCoins) = coinManager.groupedCoins

        this.featuredCoins = filteredCoins(featuredCoins)

        coins = filteredCoins(regularCoins).sortedBy { it.title.toLowerCase(Locale.ENGLISH) }
    }

    private fun filteredCoins(coins: List<Coin>): List<Coin> {
        return predefinedAccountType?.let { type ->
            coins.filter { it.type.predefinedAccountType == type }
        } ?: coins
    }

    private fun item(coin: Coin): Item {
        return Item(coin, wallets.containsKey(coin))
    }

    private fun syncState() {
        state = State(featuredCoins.map { item(it) }, coins.map { item(it) })
    }

    private fun syncCanCreate() {
        canCreateAsync.onNext(wallets.isNotEmpty())
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
