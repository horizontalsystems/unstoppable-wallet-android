package io.horizontalsystems.bankwallet.modules.market.overview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.posts.MarketPostService
import io.horizontalsystems.bankwallet.modules.market.sort
import io.horizontalsystems.core.helpers.DateHelper
import io.reactivex.disposables.CompositeDisposable

class MarketOverviewViewModel(
        private val service: MarketOverviewService,
        private val postService: MarketPostService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val topGainersViewItemsLiveData = MutableLiveData<List<MarketViewItem>>()
    val topLosersViewItemsLiveData = MutableLiveData<List<MarketViewItem>>()
    val postsViewItemsLiveData = MutableLiveData<List<MarketOverviewModule.PostViewItem>>()
    val showPoweredByLiveData = MutableLiveData(false)

    val loadingLiveData = MutableLiveData(false)
    val errorLiveData = MutableLiveData<String?>(null)
    val toastLiveData = MutableLiveData<String>()

    private val disposable = CompositeDisposable()

    init {
        service.stateObservable
                .subscribeIO {
                    syncState(it)
                }
                .let {
                    disposable.add(it)
                }

        postService.stateObservable
                .subscribeIO {
                    syncPostsState(it)
                }
                .let {
                    disposable.add(it)
                }
    }

    private fun syncPostsState(postsState: MarketPostService.State) {
        when (postsState) {
            MarketPostService.State.Loading -> { }
            MarketPostService.State.Loaded -> {
                if (service.marketItems.isNotEmpty()){
                    syncViewItemsBySortingField()
                }
            }
            is MarketPostService.State.Failed -> toastLiveData.postValue(convertErrorMessage(postsState.error))
        }
    }

    private fun syncState(state: MarketOverviewService.State) {
        val itemsEmpty = service.marketItems.isEmpty()

        when (state) {
            MarketOverviewService.State.Loading -> {
                loadingLiveData.postValue(itemsEmpty)
                errorLiveData.postValue(null)
            }
            MarketOverviewService.State.Loaded -> {
                loadingLiveData.postValue(false)
                errorLiveData.postValue(null)
                syncViewItemsBySortingField()
            }
            is MarketOverviewService.State.Error -> {
                loadingLiveData.postValue(false)
                if (itemsEmpty) {
                    errorLiveData.postValue(convertErrorMessage(state.error))
                } else {
                    toastLiveData.postValue(convertErrorMessage(state.error))
                }
            }
        }

        showPoweredByLiveData.postValue(!itemsEmpty)
    }

    private fun syncViewItemsBySortingField() {
        topGainersViewItemsLiveData.postValue(service.marketItems.sort(SortingField.TopGainers).subList(0, 5).map { MarketViewItem.create(it, MarketField.PriceDiff) })
        topLosersViewItemsLiveData.postValue(service.marketItems.sort(SortingField.TopLosers).subList(0, 5).map { MarketViewItem.create(it, MarketField.PriceDiff) })
        if(postService.newsItems.isNotEmpty()){
            val postViewItems = postService.newsItems.map {
                MarketOverviewModule.PostViewItem(getTimeAgo(it.timestamp), it.imageUrl, it.source, it.title, it.url, it.body)
            }
            postsViewItemsLiveData.postValue(postViewItems)
        }
    }

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

    private fun convertErrorMessage(it: Throwable): String {
        return it.message ?: it.javaClass.simpleName
    }

    fun onErrorClick() {
        service.refresh()
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposable.clear()
    }

    fun refresh() {
        service.refresh()
    }
}
