package io.horizontalsystems.bankwallet.modules.market.toppairs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.modules.market.overview.TopPairViewItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext

class TopPairsViewModel(private val marketKit: MarketKitWrapper) : ViewModel() {
    private var isRefreshing = false
    private var loading = false
    private var items = listOf<TopPairViewItem>()
    private var error: Throwable? = null

    var uiState by mutableStateOf(
        TopPairsUiState(
            isRefreshing = isRefreshing,
            loading = loading,
            items = items,
            error = error,
        )
    )
        private set

    init {
        loading = true
        emitState()

        viewModelScope.launch {
            fetchItems()
            loading = false
            emitState()
        }
    }

    private suspend fun fetchItems() = withContext(Dispatchers.Default) {
        try {
            error = null
            val topPairs = marketKit.topPairsSingle(1, 100).await()
            items = topPairs.map {
                TopPairViewItem.createFromTopPair(it)
            }
        } catch (e: Throwable) {
            error = e
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = TopPairsUiState(
                isRefreshing = isRefreshing,
                loading = loading,
                items = items,
                error = error,
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing = true
            emitState()

            fetchItems()
            delay(1000)
            isRefreshing = false
            emitState()
        }
    }

    fun onErrorClick() {
        refresh()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TopPairsViewModel(App.marketKit) as T
        }
    }

}

data class TopPairsUiState(
    val isRefreshing: Boolean,
    val loading: Boolean,
    val items: List<TopPairViewItem>,
    val error: Throwable?
)
