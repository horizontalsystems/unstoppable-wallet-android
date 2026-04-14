package com.quantum.wallet.bankwallet.modules.market.posts

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.providers.Translator
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.core.helpers.DateHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

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
            service.stateObservable.asFlow().collect { state ->
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
