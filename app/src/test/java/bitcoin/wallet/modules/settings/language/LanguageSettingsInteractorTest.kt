package bitcoin.wallet.modules.settings.language

import bitcoin.wallet.core.ILanguageManager
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.*

class LanguageSettingsInteractorTest {

    private val delegate = mock(LanguageSettingsModule.ILanguageSettingsInteractorDelegate::class.java)

    private val languageManager = mock(ILanguageManager::class.java)
    private val interactor = LanguageSettingsInteractor(languageManager)

    val currentLanguage : Locale = Locale("en")
    val availableLanguages: List<Locale> = listOf(Locale("en"), Locale("ru"))

    @Before
    fun before() {
        interactor.delegate = delegate

        whenever(languageManager.currentLanguage).thenReturn(currentLanguage)
        whenever(languageManager.availableLanguages).thenReturn(availableLanguages)
    }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun getItems() {
        val languageItems = availableLanguages.map {
            LanguageItem(it, it.language == currentLanguage.language)
        }

        Assert.assertEquals(interactor.items, languageItems)
    }

    @Test
    fun setCurrentLanguage() {
        val language = Locale("ru")
        val languageItem = LanguageItem(language, false)
        interactor.setCurrentLanguage(languageItem)

        verify(delegate).didSetCurrentLanguage()
    }
}