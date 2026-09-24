package io.horizontalsystems.walletkit.modules.coin

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.isSupported
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.core.order
import io.horizontalsystems.walletkit.modules.multiswap.SwapPopularTokens
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CoinViewModel(
    private val service: CoinService,
    private val clearables: List<Clearable>,
    private val marketKit: MarketKitWrapper,
    localStorage: ILocalStorage,
    private val accountManager: IAccountManager,
) : ViewModel() {

    val tabs = CoinModule.Tab.values()
    val fullCoin by service::fullCoin

    val isWatchlistEnabled = localStorage.marketsTabEnabled
    var isFavorite by mutableStateOf<Boolean>(false)
        private set
    var successMessage by mutableStateOf<Int?>(null)
        private set

    private val swappableToken: Token? = fullCoin.tokens
        .filter { it.isSupported }
        .sortedWith(
            compareBy<Token> { it.type.order }
                .thenBy { it.blockchainType.order }
        )
        .firstOrNull()

    // Drives the Buy/Sell buttons, which lead into the swap flow. A watch account cannot sign,
    // so it gets no token and the buttons stay off the screen. Only a known watch account
    // hides them: accounts load asynchronously, and "not loaded yet" is no reason to strip
    // them. Followed rather than read once, so the buttons match the account on screen.
    var coinToken by mutableStateOf(tokenForActiveAccount())
        private set

    var popularToken by mutableStateOf<Token?>(null)
        private set

    private var popularTokenJob: Job? = null

    // Null only for an account known to be a watch account; while accounts are still loading
    // the buttons stay, since hiding them on a guess would be wrong for most users.
    private fun tokenForActiveAccount() =
        swappableToken.takeIf { accountManager.activeAccount?.isWatchAccount != true }

    init {
        coinToken?.let { popularTokenJob = loadPopularToken(it) }

        viewModelScope.launch {
            accountManager.activeAccountStateFlow.collect {
                val token = tokenForActiveAccount()
                if (token == coinToken) return@collect

                coinToken = token
                popularToken = null
                popularTokenJob?.cancel()
                popularTokenJob = token?.let { loadPopularToken(it) }
            }
        }

        viewModelScope.launch {
            service.isFavorite.collect {
                isFavorite = it
            }
        }
    }

    private fun loadPopularToken(token: Token) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val popular = SwapPopularTokens.build(marketKit, token).firstOrNull()
            withContext(Dispatchers.Main) {
                popularToken = popular
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e("CoinViewModel", "Failed to build popular tokens", e)
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
