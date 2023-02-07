package cash.p.terminal.modules.market.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import cash.p.terminal.R
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.ViewState
import cash.p.terminal.entities.viewState
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.ui.compose.TranslatableString
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class MarketSearchViewModel(
    private val service: MarketSearchService
) : ViewModel() {

    private val disposables = CompositeDisposable()

    val timePeriodMenu by service::timePeriodMenu
    val sortDescending by service::sortDescending

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    var itemsData by mutableStateOf<MarketSearchModule.Data?>(null)
        private set

    var errorMessage by mutableStateOf<TranslatableString?>(null)
        private set

    init {
        service.serviceDataFlow
            .collectWith(viewModelScope) { result ->
                result.viewState?.let {
                    viewState = it
                }
                itemsData = result.getOrNull()
                errorMessage = result.exceptionOrNull()?.let { errorText(it) }
            }

        service.favoriteDataUpdated
            .subscribeIO {
                viewModelScope.launch {
                    service.updateState()
                }
            }.let {
                disposables.add(it)
            }

        viewModelScope.launch {
            service.updateState()
        }
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun refresh() {
        viewModelScope.launch {
            service.updateState()
        }
    }

    fun searchByQuery(query: String) {
        viewModelScope.launch {
            service.setFilter(query.trim())
        }
    }

    fun onFavoriteClick(favourited: Boolean, coinUid: String) {
        if (favourited) {
            service.unFavorite(coinUid)
        } else {
            service.favorite(coinUid)
        }
    }

    fun toggleTimePeriod(timeDuration: TimeDuration) {
        viewModelScope.launch {
            service.setTimePeriod(timeDuration)
        }
    }

    fun toggleSortType() {
        viewModelScope.launch {
            service.toggleSortType()
        }
    }

    fun errorShown() {
        errorMessage = null
    }

    private fun errorText(error: Throwable): TranslatableString {
        return when (error) {
            is UnknownHostException -> TranslatableString.ResString(R.string.Hud_Text_NoInternet)
            else -> TranslatableString.PlainString(error.message ?: error.javaClass.simpleName)
        }
    }

}
