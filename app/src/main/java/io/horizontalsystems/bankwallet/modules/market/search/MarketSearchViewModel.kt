package io.horizontalsystems.bankwallet.modules.market.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.viewState
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
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
