package io.horizontalsystems.bankwallet.modules.coin

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.isSupported
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.modules.multiswap.SwapPopularTokens
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.withContext

class CoinViewModel(
    private val service: CoinService,
    private val clearables: List<Clearable>,
    private val marketKit: MarketKitWrapper,
    localStorage: ILocalStorage,
) : ViewModel() {

    val tabs = CoinModule.Tab.values()
    val fullCoin by service::fullCoin

    val isWatchlistEnabled = localStorage.marketsTabEnabled
    var isFavorite by mutableStateOf<Boolean>(false)
        private set
    var successMessage by mutableStateOf<Int?>(null)
        private set

     val coinToken: Token? = fullCoin.tokens
        .filter { it.isSupported }
        .sortedWith(
            compareBy<Token> { it.type.order }
                .thenBy { it.blockchainType.order }
        )
        .firstOrNull()

    var popularToken by mutableStateOf<Token?>(null)
        private set

    init {
        coinToken?.let { context ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val token = SwapPopularTokens.build(marketKit, context).firstOrNull()
                    withContext(Dispatchers.Main) {
                        popularToken = token
                    }
                } catch (e: Throwable) {
                    Log.e("CoinViewModel", "Failed to build popular tokens", e)
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
