package io.horizontalsystems.bankwallet.modules.backup

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IRandomProvider
import io.horizontalsystems.bankwallet.core.managers.AuthManager
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class BackupInteractorTest {

    private val authManager = mock(AuthManager::class.java)
    private val wordsManager = mock(WordsManager::class.java)
    private val delegate = mock(BackupModule.IInteractorDelegate::class.java)
    private val indexesProvider = mock(IRandomProvider::class.java)
    private val localStorage = mock(ILocalStorage::class.java)
    private val keystoreSafeExecute = mock(IKeyStoreSafeExecute::class.java)

    private lateinit var interactor: BackupInteractor

    @Before
    fun setup() {
        RxBaseTest.setup()

        interactor = BackupInteractor(authManager, wordsManager, indexesProvider, localStorage, keystoreSafeExecute)
        interactor.delegate = delegate
    }

    @Test
    fun fetchWords() {
        val words = listOf("1", "2", "etc")
        val authData = mock(AuthData::class.java)

        whenever(authData.words).thenReturn(words)
        whenever(authManager.authData).thenReturn(authData)

        interactor.fetchWords()

        verify(delegate).didFetchWords(words)
    }

    @Test
    fun fetchConfirmationIndexes() {
        val indexes = listOf(1, 3)
        whenever(indexesProvider.getRandomIndexes(2)).thenReturn(indexes)

        interactor.fetchConfirmationIndexes()
        verify(delegate).didFetchConfirmationIndexes(indexes)
    }

    @Test
    fun validate_success() {
        val validatedHashmap = hashMapOf(1 to "apple", 2 to "2", 3 to "lemon")
        val words = listOf("apple", "2", "lemon")
        val authData = mock(AuthData::class.java)

        whenever(authData.words).thenReturn(words)
        whenever(authManager.authData).thenReturn(authData)

        interactor.validate(validatedHashmap)

        verify(wordsManager).isBackedUp = true
        verify(delegate).didValidateSuccess()
    }

    @Test
    fun validate_failure() {
        val validatedHashmap = hashMapOf(1 to "apple", 3 to "lemon")
        val words = listOf("tree", "2", "lemon")
        val authData = mock(AuthData::class.java)

        whenever(authData.words).thenReturn(words)
        whenever(authManager.authData).thenReturn(authData)

        interactor.validate(validatedHashmap)

        verify(delegate).didValidateFailure()
    }
}
