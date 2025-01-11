package cash.p.terminal.modules.coin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.wallet.Clearable
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.wallet.SubscriptionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class CoinViewModel(
    private val service: CoinService,
    private val clearables: List<Clearable>,
    private val localStorage: ILocalStorage,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    val tabs = CoinModule.Tab.values()
    val fullCoin by service::fullCoin

    val isWatchlistEnabled = localStorage.marketsTabEnabled
    var isFavorite by mutableStateOf<Boolean>(false)
        private set
    var successMessage by mutableStateOf<Int?>(null)
        private set

    private var subscriptionInfoShown: Boolean = false

    init {
        viewModelScope.launch {
            val isFavoriteFlow: Flow<Boolean> = service.isFavorite.asFlow()
            isFavoriteFlow.collect {
                isFavorite = it
            }
        }
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
    }

    fun onFavoriteClick() {
        service.favorite()
        successMessage = R.string.Hud_Added_To_Watchlist
    }

    fun onUnfavoriteClick() {
        service.unfavorite()
        successMessage = R.string.Hud_Removed_from_Watchlist
    }

    fun onSuccessMessageShown() {
        successMessage = null
    }

    fun shouldShowSubscriptionInfo():Boolean {
        return !subscriptionManager.hasSubscription() && !subscriptionInfoShown
    }

    fun subscriptionInfoShown() {
        subscriptionInfoShown = true
    }

}
