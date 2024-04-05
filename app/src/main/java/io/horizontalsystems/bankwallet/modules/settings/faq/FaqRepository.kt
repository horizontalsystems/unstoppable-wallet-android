package io.horizontalsystems.bankwallet.modules.settings.faq

import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.FaqManager
import io.horizontalsystems.bankwallet.core.managers.LanguageManager
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.FaqMap
import io.horizontalsystems.bankwallet.entities.FaqSection
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import java.util.concurrent.TimeUnit

class FaqRepository(
    private val faqManager: FaqManager,
    private val connectivityManager: ConnectivityManager,
    private val languageManager: LanguageManager
) {

    val faqList: Observable<DataState<List<FaqSection>>>
        get() = faqListSubject

    private val faqListSubject = BehaviorSubject.create<DataState<List<FaqSection>>>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val retryLimit = 3

    fun start() {
        fetch()

        coroutineScope.launch {
            connectivityManager.networkAvailabilitySignal.asFlow().collect {
                if (connectivityManager.isConnected && faqListSubject.value is DataState.Error) {
                    fetch()
                }
            }
        }
    }

    fun clear() {
        coroutineScope.cancel()
    }

    private fun fetch() {
        faqListSubject.onNext(DataState.Loading)

        coroutineScope.launch {
            try {
                val faqMaps = faqManager.getFaqList()
                    .retryWhen { errors -> // retry on error java.lang.AssertionError: No System TLS
                        errors.zipWith(
                            Flowable.range(1, retryLimit + 1)
                        ) { error: Throwable, retryCount: Int ->
                            if (retryCount < retryLimit && (error is AssertionError)) {
                                retryCount
                            } else {
                                throw error
                            }
                        }.flatMap {
                            Flowable.timer(1, TimeUnit.SECONDS)
                        }
                    }.await()

                val faqSections = getByLocalLanguage(
                    faqMaps,
                    languageManager.currentLocale.language,
                    languageManager.fallbackLocale.language
                )
                faqListSubject.onNext(DataState.Success(faqSections))
            } catch (e: Throwable) {
                faqListSubject.onNext(DataState.Error(e))
            }
        }
    }

    private fun getByLocalLanguage(
        faqMultiLanguage: List<FaqMap>,
        language: String,
        fallbackLanguage: String
    ) =
        faqMultiLanguage.map { sectionMultiLang ->
            val categoryTitle = sectionMultiLang.section[language]
                ?: sectionMultiLang.section[fallbackLanguage]
                ?: ""
            val sectionItems =
                sectionMultiLang.items.mapNotNull { it[language] ?: it[fallbackLanguage] }

            FaqSection(categoryTitle, sectionItems)
        }
}
