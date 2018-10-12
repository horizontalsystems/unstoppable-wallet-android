package bitcoin.wallet.modules.backup

import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.core.IRandomProvider
import bitcoin.wallet.core.managers.WordsManager
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Captor
import org.mockito.Mockito.mock





class BackupInteractorTest {

    private val wordsManager = mock(WordsManager::class.java)
    private val delegate = mock(BackupModule.IInteractorDelegate::class.java)
    private val randomProvider = mock(IRandomProvider::class.java)
    private val keystoreSafeExecute = mock(IKeyStoreSafeExecute::class.java)
    private val interactor = BackupInteractor(wordsManager, randomProvider, keystoreSafeExecute)

    @Captor
    private val actionRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()

    @Captor
    private val successRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()

    @Captor
    private val failureRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor.delegate = delegate
    }

    @Test
    fun fetchWords() {
        val words = listOf("1", "2", "etc")
        whenever(wordsManager.savedWords()).thenReturn(words)

        interactor.fetchWords()

        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue

        actionRunnable.run()

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
        whenever(wordsManager.savedWords()).thenReturn(words)

        interactor.validate(validatedHashmap)

        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue

        actionRunnable.run()

        verify(wordsManager).wordListBackedUp = true
        verify(delegate).didValidateSuccess()
    }

    @Test
    fun validate_failure() {
        val validatedHashmap = hashMapOf(1 to "apple", 3 to "lemon")
        val words = listOf("tree", "2", "lemon")
        whenever(wordsManager.savedWords()).thenReturn(words)

        interactor.validate(validatedHashmap)

        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue

        actionRunnable.run()

        verify(delegate).didValidateFailure()
    }
}
