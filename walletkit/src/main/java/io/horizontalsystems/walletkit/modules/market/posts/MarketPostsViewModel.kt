package io.horizontalsystems.walletkit.modules.market.posts

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.collectSafely
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.ViewState
import io.horizontalsystems.walletkit.helpers.DateHelper
import kotlinx.coroutines.launch

class MarketPostsViewModel(private val service: MarketPostService) : ViewModel() {

    val itemsLiveData = MutableLiveData<List<MarketPostsModule.PostViewItem>>()
    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val isRefreshingLiveData = MutableLiveData<Boolean>()

    private fun getTimeAgo(timestamp: Long): String {
        val secondsAgo = DateHelper.getSecondsAgo(timestamp * 1000)
        val minutesAgo = secondsAgo / 60
        val hoursAgo = minutesAgo / 60

        return when {
            // interval from post in minutes
            minutesAgo < 60 -> Translator.getString(R.string.Market_MinutesAgo, minutesAgo)
            hoursAgo < 24 -> Translator.getString(R.string.Market_HoursAgo, hoursAgo)
            else -> {
                val daysAgo = hoursAgo / 24
                Translator.getString(R.string.Market_DaysAgo, daysAgo)
            }
        }
    }

    init {
        viewModelScope.launch {
            service.stateFlow.collectSafely { state ->
                isRefreshingLiveData.postValue(false)

                state.dataOrNull?.let { posts ->
                    val postViewItems = posts.map {
                        MarketPostsModule.PostViewItem(
                            source = it.source.replaceFirstChar(Char::titlecase),
                            title = it.title,
                            body = it.body,
                            timeAgo = getTimeAgo(it.timestamp),
                            url = it.url
                        )
                    }
                    itemsLiveData.postValue(postViewItems)
                }

                state.viewState?.let {
                    viewStateLiveData.postValue(it)
                }
            }
        }

        service.start()
    }

    override fun onCleared() {
        service.stop()
    }

    fun onErrorClick() {
        service.refresh()
    }

    fun refresh() {
        isRefreshingLiveData.postValue(true)
        service.refresh()
    }
}
