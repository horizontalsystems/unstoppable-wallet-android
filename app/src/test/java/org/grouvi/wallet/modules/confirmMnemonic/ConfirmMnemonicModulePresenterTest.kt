package org.grouvi.wallet.modules.confirmMnemonic

import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class ConfirmMnemonicModulePresenterTest {

    private val presenter = ConfirmMnemonicModulePresenter()
    private val interactor = mock(ConfirmMnemonicModule.IInteractor::class.java)
    private val view = mock(ConfirmMnemonicModule.IView::class.java)

    @Before
    fun before() {
        presenter.interactor = interactor
        presenter.view = view
    }

    @Test
    fun start() {
        presenter.start()

        interactor.retrieveConfirmationWord()
    }

    @Test
    fun didConfirmationWordRetrieve() {
        presenter.didConfirmationWordRetrieve(5)

        verify(view).showWordConfirmationForm(5)
    }

    @Test
    fun submit() {
        val word = "piano"
        val position = 4

        presenter.submit(position, word)

        verify(interactor).validateConfirmationWord(position, word)
    }

    @Test
    fun didConfirmationFailure() {
        presenter.didConfirmationFailure()

        verify(view).showWordNotConfirmedError()
    }

}