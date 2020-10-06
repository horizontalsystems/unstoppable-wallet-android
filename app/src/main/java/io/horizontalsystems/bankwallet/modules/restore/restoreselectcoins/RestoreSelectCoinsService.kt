package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IDerivationSettingsManager
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.reactivex.subjects.BehaviorSubject

class RestoreSelectCoinsService(
        private val predefinedAccountType: PredefinedAccountType,
        private val coinManager: ICoinManager,
        private val derivationSettingsManager: IDerivationSettingsManager
) : RestoreSelectCoinsModule.IService, Clearable {

    override val canRestore = BehaviorSubject.create<Boolean>()
    override val stateObservable = BehaviorSubject.create<State>()
    override var state: State = State()
        set(value) {
            field = value
            stateObservable.onNext(value)
        }

    override var enabledCoins: MutableList<Coin> = mutableListOf()
        private set


    init {
        syncState()
    }

    override fun enable(coin: Coin, derivationSetting: DerivationSetting?) {
        val coinDerivationSetting = derivationSettingsManager.derivationSetting(coin.type) ?: derivationSettingsManager.defaultDerivationSetting(coin.type)
        coinDerivationSetting?.let { setting ->
            derivationSetting ?: throw EnableCoinError.DerivationNotConfirmed(setting.derivation)

            derivationSettingsManager.updateSetting(derivationSetting)
        }

        enabledCoins.add(coin)

        syncState()
        syncCanRestore()
    }

    override fun disable(coin: Coin) {
        enabledCoins.remove(coin)

        syncState()
        syncCanRestore()
    }

    override fun clear() {
    }

    private fun filteredCoins(coins: List<Coin>): List<Coin> {
        return coins.filter { it.type.predefinedAccountType == predefinedAccountType }
    }

    private fun item(coin: Coin): Item? {
        return Item(coin, enabledCoins.contains(coin))
    }

    private fun syncState() {
        val featuredCoins = filteredCoins(coinManager.featuredCoins)
        val coins = filteredCoins(coinManager.coins).filter { !featuredCoins.contains(it) }

        state = State(featuredCoins.mapNotNull { item(it) }, coins.mapNotNull { item(it) })
    }

    private fun syncCanRestore() {
        canRestore.onNext(enabledCoins.isNotEmpty())
    }


    data class State(var featured: List<Item>, var items: List<Item>) {
        constructor() : this(listOf(), listOf())
    }

    data class Item(val coin: Coin, val enabled: Boolean)

    sealed class EnableCoinError : Exception() {
        class DerivationNotConfirmed(val currentDerivation: AccountType.Derivation) : EnableCoinError()
    }
}
