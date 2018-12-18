package io.horizontalsystems.bankwallet.modules.restore

import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.WordsManager
import org.junit.Before
import org.junit.Test
import org.mockito.Captor
import org.mockito.Mockito.*

class RestoreInteractorTest {

    private val wordsManager = mock(WordsManager::class.java)
    private val delegate = mock(RestoreModule.IInteractorDelegate::class.java)
    private val keystoreSafeExecute = mock(IKeyStoreSafeExecute::class.java)
    private val localStorage = mock(ILocalStorage::class.java)

    private lateinit var interactor: RestoreInteractor

    @Captor
    private val actionRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()
    @Captor
    private val successRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()
    @Captor
    private val failureRunnableCaptor: KArgumentCaptor<Runnable> = argumentCaptor()

    @Before
    fun before() {
        interactor = RestoreInteractor(wordsManager, localStorage, keystoreSafeExecute)
        interactor.delegate = delegate
    }

    @Test
    fun restoreWallet_restore() {
        val words = listOf("first", "second", "etc")

        interactor.restore(words)

        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue
        val successRunnable = successRunnableCaptor.firstValue

        actionRunnable.run()
        successRunnable.run()

        verify(wordsManager).restore(words)
    }

    @Test
    fun restoreWallet_success() {
        val words = listOf("first", "second", "etc")

        interactor.restore(words)

        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue
        val successRunnable = successRunnableCaptor.firstValue

        actionRunnable.run()
        successRunnable.run()

        verify(delegate).didRestore()
        verify(wordsManager).isBackedUp = true
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun restoreWallet_failureWordsError() {
        val words = listOf("first", "second", "etc")

        interactor.restore(words)

        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue
        val failureRunnable = failureRunnableCaptor.firstValue

        actionRunnable.run()
        failureRunnable.run()

        verify(delegate).didFailToRestore(any())
        verifyNoMoreInteractions(delegate)
    }

}
