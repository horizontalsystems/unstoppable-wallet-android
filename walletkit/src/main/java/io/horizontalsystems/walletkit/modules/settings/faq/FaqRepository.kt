package io.horizontalsystems.walletkit.modules.settings.faq

import io.horizontalsystems.walletkit.core.managers.ConnectivityManager
import io.horizontalsystems.walletkit.core.managers.FaqManager
import io.horizontalsystems.walletkit.core.managers.LanguageManager
import io.horizontalsystems.walletkit.core.retryWhen
import io.horizontalsystems.walletkit.entities.DataState
import io.horizontalsystems.walletkit.entities.FaqMap
import io.horizontalsystems.walletkit.entities.FaqSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FaqRepository(
    private val faqManager: FaqManager,
    private val connectivityManager: ConnectivityManager,
    private val languageManager: LanguageManager
) {

    private val _faqList = MutableStateFlow<DataState<List<FaqSection>>>(DataState.Loading)

    val faqList: StateFlow<DataState<List<FaqSection>>>
        get() = _faqList
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val retryLimit = 3

    fun start() {
        fetch()

        coroutineScope.launch {
            connectivityManager.networkAvailabilityFlow.collect {
                if (connectivityManager.isConnected && _faqList.value is DataState.Error) {
                    fetch()
                }
            }
        }
    }

    fun clear() {
        coroutineScope.cancel()
    }

    private fun fetch() {
        _faqList.tryEmit(DataState.Loading)

        coroutineScope.launch {
            try {
                val faqMaps = retryWhen(
                    times = retryLimit,
                    predicate = { it is AssertionError }
                ) {
                    faqManager.getFaqList()
                }

                val faqSections = getByLocalLanguage(
                    faqMaps,
                    languageManager.currentLocale.language,
                    languageManager.fallbackLocale.language
                )
                _faqList.tryEmit(DataState.Success(faqSections))
            } catch (e: Throwable) {
                _faqList.tryEmit(DataState.Error(e))
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
