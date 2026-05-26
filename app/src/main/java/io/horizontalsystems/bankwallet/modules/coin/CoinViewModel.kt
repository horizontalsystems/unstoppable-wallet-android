package io.horizontalsystems.bankwallet.modules.coin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

@HiltViewModel(assistedFactory = CoinViewModel.Factory::class)
class CoinViewModel @AssistedInject constructor(
    @Assisted private val coinUid: String,
    private val marketKit: MarketKitWrapper,
    private val marketFavoritesManager: MarketFavoritesManager,
    private val localStorage: ILocalStorage,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(coinUid: String): CoinViewModel
    }

    private val service = CoinService(
        marketKit.fullCoins(coinUids = listOf(coinUid)).first(),
        marketFavoritesManager,
    )

    val tabs = CoinModule.Tab.values()
    val fullCoin by service::fullCoin

    val isWatchlistEnabled = localStorage.marketsTabEnabled
    var isFavorite by mutableStateOf<Boolean>(false)
        private set
    var successMessage by mutableStateOf<Int?>(null)
        private set

    init {
        viewModelScope.launch {
            val isFavoriteFlow: Flow<Boolean> = service.isFavorite.asFlow()
            isFavoriteFlow.collect {
                isFavorite = it
            }
        }
    }

    override fun onCleared() {
        service.clear()
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

}
