package bitcoin.wallet.modules.backup

import bitcoin.wallet.core.IRandomProvider
import bitcoin.wallet.core.managers.WordsManager
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify

class BackupInteractorTest {

    private val wordsManager = Mockito.mock(WordsManager::class.java)
    private val delegate = Mockito.mock(BackupModule.IInteractorDelegate::class.java)
    private val randomProvider = Mockito.mock(IRandomProvider::class.java)
    private val interactor = BackupInteractor(wordsManager, randomProvider)

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor.delegate = delegate
    }

    @Test
    fun fetchWords() {
        val words = listOf("1", "2", "etc")
        whenever(wordsManager.savedWords).thenReturn(words)

        interactor.fetchWords()
        verify(delegate).didFetchWords(words)
    }

    @Test
    fun fetchConfirmationIndexes() {
        val indexes = listOf(1, 3)
        whenever(randomProvider.getRandomIndexes(2)).thenReturn(indexes)

        interactor.fetchConfirmationIndexes()
        verify(delegate).didFetchConfirmationIndexes(indexes)
    }

    @Test
    fun validate_success() {
        val validatedHashmap = hashMapOf(1 to "apple", 3 to "lemon")
        val words = listOf("apple", "2", "lemon")
        whenever(wordsManager.savedWords).thenReturn(words)

        interactor.validate(validatedHashmap)
        verify(wordsManager).wordListBackedUp = true
        verify(delegate).didValidateSuccess()
    }

    @Test
    fun validate_failure() {
        val validatedHashmap = hashMapOf(1 to "apple", 3 to "lemon")
        val words = listOf("tree", "2", "lemon")
        whenever(wordsManager.savedWords).thenReturn(words)

        interactor.validate(validatedHashmap)
        verify(delegate).didValidateFailure()
    }
}
