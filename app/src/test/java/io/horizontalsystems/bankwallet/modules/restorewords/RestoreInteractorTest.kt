package io.horizontalsystems.bankwallet.modules.restorewords

import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsInteractor
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsModule
import io.horizontalsystems.hdwalletkit.Mnemonic
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class RestoreInteractorTest {

    private val wordsManager = mock(WordsManager::class.java)
    private val delegate = mock(RestoreWordsModule.InteractorDelegate::class.java)
    private lateinit var interactor: RestoreWordsInteractor

    @Before
    fun before() {
        interactor = RestoreWordsInteractor(wordsManager)
        interactor.delegate = delegate
    }

    @Test
    fun validate() {
        val words = listOf("yahoo", "google", "facebook")
        interactor.validate(words)

        verify(delegate).didValidate()
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
