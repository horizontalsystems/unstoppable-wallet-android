package io.horizontalsystems.bankwallet.modules.settings.faq

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.FaqMap
import io.horizontalsystems.core.ILanguageManager
import io.horizontalsystems.views.ListPosition
import io.reactivex.disposables.CompositeDisposable

class FaqViewModel(private val repository: FaqRepository, private val languageManager: ILanguageManager) : ViewModel() {

    val faqItemList = MutableLiveData<List<FaqData>>()
    val loading = MutableLiveData(false)
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

    private fun didFetchFaqList(faqMap: Array<FaqMap>) {
        val currentLang = languageManager.currentLocale.language
        val fallbackLang = languageManager.fallbackLocale.language

        val items = mutableListOf<FaqData>()

        for (map in faqMap) {
            val section = map.section[currentLang] ?: map.section[fallbackLang] ?: continue
            val sectionItems = map.items.mapNotNull { it[currentLang] ?: it[fallbackLang] }

            items.add(FaqSection(section))

            sectionItems.forEachIndexed { index, faq ->
                items.add(FaqItem(faq, ListPosition.getListPosition(sectionItems.size, index)))
            }
        }

        faqItemList.postValue(items)
    }
}
