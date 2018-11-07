package io.horizontalsystems.bankwallet.modules.settings.language

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.*

class LanguageSettingsPresenterTest {

    private val interactor = mock(LanguageSettingsModule.ILanguageSettingsInteractor::class.java)
    private val router = mock(LanguageSettingsModule.ILanguageSettingsRouter::class.java)
    private val view = mock(LanguageSettingsModule.ILanguageSettingsView::class.java)

    val items = listOf(LanguageItem(Locale("en"), true), LanguageItem(Locale("ru"), false))

    private val presenter = LanguageSettingsPresenter(router, interactor)

    @Before
    fun setUp() {
        presenter.view = view
        whenever(interactor.items).thenReturn(items)
    }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(view)
        verifyNoMoreInteractions(router)
    }

    @Test
    fun didSetCurrentLanguage() {
        presenter.didSetCurrentLanguage()
        verify(router).reloadAppInterface()
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()
        verify(view).setTitle(any())
        verify(view).show(items)
    }

    @Test
    fun didSelect() {
        val item = items[0]
        presenter.didSelect(item)
        verify(interactor).setCurrentLanguage(item)
    }
}