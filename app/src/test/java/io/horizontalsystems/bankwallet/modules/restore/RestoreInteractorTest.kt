package io.horizontalsystems.bankwallet.modules.restore

import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.hdwalletkit.Mnemonic
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class RestoreInteractorTest {

    private val wordsManager = mock(WordsManager::class.java)
    private val delegate = mock(RestoreModule.IInteractorDelegate::class.java)
    private lateinit var interactor: RestoreInteractor

    @Before
    fun before() {
        interactor = RestoreInteractor(wordsManager)
        interactor.delegate = delegate
    }

    @Test
    fun validate() {
        val words = listOf("yahoo", "google", "facebook")
        interactor.validate(words)
        verify(delegate).didValidate(words)
    }

    @Test
    fun validate_failed() {
        val words = listOf("yahoo", "google", "facebook")
        val mnemonicException = Mnemonic.MnemonicException("error")
        whenever(wordsManager.validate(words)).thenThrow(mnemonicException)
        interactor.validate(words)
        verify(delegate).didFailToValidate(mnemonicException)
    }

}
