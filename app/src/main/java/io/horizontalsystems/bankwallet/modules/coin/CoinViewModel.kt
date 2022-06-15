package io.horizontalsystems.bankwallet.modules.coin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class CoinViewModel(
    private val service: CoinService,
    private val clearables: List<Clearable>,
) : ViewModel() {

    val tabs = CoinModule.Tab.values()
    val fullCoin by service::fullCoin

    var isFavorite by mutableStateOf<Boolean>(false)
        private set
    var coinState by mutableStateOf<CoinState?>(null)
        private set
    var successMessage by mutableStateOf<Int?>(null)
        private set

    init {
        viewModelScope.launch {
            val coinStateFlow: Flow<CoinState> = service.coinState.asFlow()
            coinStateFlow.collect {
                coinState = it

                if (it == CoinState.AddedToWallet) {
                    successMessage = R.string.Hud_Added_To_Wallet
                }
            }
        }

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

}
