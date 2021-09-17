package io.horizontalsystems.bankwallet.modules.market.posts

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.core.helpers.DateHelper
import io.reactivex.disposables.CompositeDisposable

class MarketPostsViewModel(
    private val service: MarketPostService,
    private val clearables: List<Clearable>
) : ViewModel() {

    val postsViewItemsLiveData = MutableLiveData<List<MarketPostsModule.PostViewItem>>()
    val toastLiveData = SingleLiveEvent<String>()
    val loadingLiveData = MutableLiveData(false)
    val errorLiveData = MutableLiveData<String?>(null)

    private val disposable = CompositeDisposable()

    init {
        service.stateObservable
            .subscribeIO {
                syncPostsState(it)
            }
            .let {
                disposable.add(it)
            }
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposable.clear()
    }

    fun onErrorClick() {
        service.refresh()
    }

    private fun syncPostsState(state: MarketPostService.State) {
        val itemsEmpty = service.newsItems.isEmpty()

        when (state) {
            MarketPostService.State.Loading -> {
                errorLiveData.postValue(null)
                loadingLiveData.postValue(itemsEmpty)
            }
            MarketPostService.State.Loaded -> {
                if (service.newsItems.isNotEmpty()) {
                    val postViewItems = service.newsItems.map {
                        MarketPostsModule.PostViewItem(
                            getTimeAgo(it.timestamp),
                            it.imageUrl,
                            it.source,
                            it.title,
                            it.url,
                            it.body
                        )
                    }
                    errorLiveData.postValue(null)
                    loadingLiveData.postValue(false)
                    postsViewItemsLiveData.postValue(postViewItems)
                }
            }
            is MarketPostService.State.Failed -> {
                loadingLiveData.postValue(false)
                if (itemsEmpty) {
                    errorLiveData.postValue(convertErrorMessage(state.error))
                } else {
                    toastLiveData.postValue(convertErrorMessage(state.error))
                }
            }
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

    fun refresh() {
        service.refresh()
    }
}
