package io.horizontalsystems.walletkit.modules.settings.guides

import io.horizontalsystems.walletkit.core.managers.ConnectivityManager
import io.horizontalsystems.walletkit.core.managers.GuidesManager
import io.horizontalsystems.walletkit.core.managers.LanguageManager
import io.horizontalsystems.walletkit.core.retryWhen
import io.horizontalsystems.walletkit.entities.DataState
import io.horizontalsystems.walletkit.entities.GuideCategory
import io.horizontalsystems.walletkit.entities.GuideCategoryMultiLang
import io.horizontalsystems.walletkit.entities.GuideSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GuidesRepository(
        private val guidesManager: GuidesManager,
        private val connectivityManager: ConnectivityManager,
        private val languageManager: LanguageManager
        ) {

    val guideCategories: StateFlow<DataState<List<GuideCategory>>>
        get() = _guideCategories

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val _guideCategories = MutableStateFlow<DataState<List<GuideCategory>>>(DataState.Loading)
    private val retryLimit = 3

    init {
        fetch()

        coroutineScope.launch {
            connectivityManager.networkAvailabilityFlow.collect {
                if (connectivityManager.isConnected && _guideCategories.value is DataState.Error) {
                    fetch()
                }
            }
        }
    }

    fun clear() {
        coroutineScope.cancel()
    }

    private fun fetch() {
        _guideCategories.tryEmit(DataState.Loading)

        coroutineScope.launch {
            try {
                val guideCategories = retryWhen(
                    times = retryLimit,
                    predicate = { it is AssertionError }
                ) {
                    guidesManager.getGuideCategories()
                }

                val categories = getCategoriesByLocalLanguage(guideCategories, languageManager.currentLocale.language, languageManager.fallbackLocale.language)
                _guideCategories.tryEmit(DataState.Success(categories))
            } catch (e: Throwable) {
                _guideCategories.tryEmit(DataState.Error(e))
            }
        }
    }

    private fun getCategoriesByLocalLanguage(categoriesMultiLanguage: Array<GuideCategoryMultiLang>, language: String, fallbackLanguage: String) =
        categoriesMultiLanguage.map { categoriesMultiLang ->
            val categoryTitle = categoriesMultiLang.category[language] ?: categoriesMultiLang.category[fallbackLanguage] ?: ""

            val sections = categoriesMultiLang.sections.map { sectionMultiLang ->
                val sectionTitle = sectionMultiLang.title[language] ?: sectionMultiLang.title[fallbackLanguage] ?: ""
                val items = sectionMultiLang.items.mapNotNull {
                    it[language] ?: it[fallbackLanguage]
                }
                GuideSection(sectionTitle, items)
            }
            GuideCategory(categoryTitle, sections)
        }
}
