package io.horizontalsystems.bankwallet.modules.managewallets

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.bankwallet.modules.managewallets.view.ManageWalletsFragment
import io.horizontalsystems.bankwallet.modules.managewallets.view.ManageWalletsViewModel
import io.reactivex.Observable

object ManageWalletsModule {

    interface IManageWalletsService {
        val state: State
        val stateObservable: Observable<Unit>
        fun enable(coin: Coin, derivationSetting: DerivationSetting? = null)
        fun disable(coin: Coin)
    }

    data class State(val featuredItems: List<Item>, val items: List<Item>) {

        companion object {
            fun empty(): State {
                return State(listOf(), listOf())
            }
        }
    }

    class Item(val coin: Coin, var state: ItemState)

    sealed class ItemState {
        object NoAccount : ItemState()
        class HasAccount(val hasWallet: Boolean) : ItemState()
    }


    class Factory() : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = ManageWalletsService(App.coinManager, App.walletManager, App.accountManager, App.derivationSettingsManager)

            return ManageWalletsViewModel(service, listOf(service)) as T
        }
    }

    fun start(activity: FragmentActivity) {
        ManageWalletsFragment.start(activity)
    }

}
