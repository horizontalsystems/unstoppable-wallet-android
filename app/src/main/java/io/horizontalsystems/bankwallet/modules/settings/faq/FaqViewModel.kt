package io.horizontalsystems.bankwallet.modules.settings.faq

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Faq
import io.horizontalsystems.views.ListPosition
import io.reactivex.disposables.CompositeDisposable

class FaqViewModel(private val repository: FaqRepository): ViewModel() {

    val faqItemList = MutableLiveData<List<FaqItem>>()
    val loading = MutableLiveData<Boolean>(false)
    val error = MutableLiveData<Throwable?>()

    private var disposables = CompositeDisposable()

    init {
        repository.faqList
                .subscribe {
                    loading.postValue(it is DataState.Loading)

                    if (it is DataState.Success) {
                        didFetchFaqList(it.data)
                    }

                    error.postValue((it as? DataState.Error)?.throwable)
                }
                .let {
                    disposables.add(it)
                }
    }

    override fun onCleared() {
        disposables.dispose()

        repository.clear()
    }

    private fun didFetchFaqList(faqArray: Array<Faq>) {
        val faqItems = faqArray.mapIndexed { index, faq ->
            FaqItem(faq, ListPosition.getListPosition(faqArray.size, index))
        }
        faqItemList.postValue(faqItems)
    }
}
