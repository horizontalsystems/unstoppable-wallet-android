package org.grouvi.wallet.modules.generateMnemonic

import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class GenerateMnemonicModulePresenterTest {
    private val presenter = GenerateMnemonicModulePresenter()
    private val interactor = mock(GenerateMnemonicModule.IInteractor::class.java)
    private val view = mock(GenerateMnemonicModule.IView::class.java)
    private val router = mock(GenerateMnemonicModule.IRouter::class.java)

    @Before
    fun before() {
        presenter.interactor = interactor
        presenter.view = view
        presenter.router = router
    }

    @Test
    fun start() {
        presenter.start()

        verify(interactor).generateMnemonic()
    }

    @Test
    fun didGenerateMnemonic() {
        val words = listOf("one", "two", "...")

        presenter.didGenerateMnemonic(words)

        verify(view).showMnemonicWords(words)
    }

    @Test
    fun next() {
        presenter.complete()

        verify(router).openMnemonicWordsConfirmation()
    }
}