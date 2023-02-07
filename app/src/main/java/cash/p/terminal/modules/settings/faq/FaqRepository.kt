package cash.p.terminal.modules.settings.faq

import cash.p.terminal.core.managers.ConnectivityManager
import cash.p.terminal.core.managers.FaqManager
import cash.p.terminal.core.managers.LanguageManager
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.FaqMap
import cash.p.terminal.entities.FaqSection
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

class FaqRepository(
    private val faqManager: FaqManager,
    private val connectivityManager: ConnectivityManager,
    private val languageManager: LanguageManager
) {

    val faqList: Observable<DataState<List<FaqSection>>>
        get() = faqListSubject

    private val faqListSubject = BehaviorSubject.create<DataState<List<FaqSection>>>()
    private val disposables = CompositeDisposable()
    private val retryLimit = 3

    fun start() {
        fetch()

        connectivityManager.networkAvailabilitySignal
            .subscribeOn(Schedulers.io())
            .subscribe {
                if (connectivityManager.isConnected && faqListSubject.value is DataState.Error) {
                    fetch()
                }
            }
            .let {
                disposables.add(it)
            }
    }

    fun clear() {
        disposables.clear()
    }

    private fun fetch() {
        faqListSubject.onNext(DataState.Loading)

        faqManager.getFaqList()
            .retryWhen { errors -> // retry on error java.lang.AssertionError: No System TLS
                errors.zipWith(
                    Flowable.range(1, retryLimit + 1),
                    { error: Throwable, retryCount: Int ->
                        if (retryCount < retryLimit && (error is AssertionError)) {
                            retryCount
                        } else {
                            throw error
                        }
                    }
                ).flatMap {
                    Flowable.timer(1, TimeUnit.SECONDS)
                }
            }
            .subscribeOn(Schedulers.io())
            .subscribe({
                val faqSections = getByLocalLanguage(
                    it,
                    languageManager.currentLocale.language,
                    languageManager.fallbackLocale.language
                )
                faqListSubject.onNext(DataState.Success(faqSections))
            }, {
                faqListSubject.onNext(DataState.Error(it))
            })
            .let {
                disposables.add(it)
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
