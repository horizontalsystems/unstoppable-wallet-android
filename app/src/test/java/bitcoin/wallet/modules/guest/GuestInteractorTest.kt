package bitcoin.wallet.modules.guest

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.IKeyStoreSafeExecute
import bitcoin.wallet.core.managers.WordsManager
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Captor
import org.mockito.Mockito
import org.mockito.Mockito.*

class GuestInteractorTest {

    private val wordsManager = mock(WordsManager::class.java)
    private val delegate = mock(GuestModule.IInteractorDelegate::class.java)
    private val adapterManager = mock(AdapterManager::class.java)
    private val keystoreSafeExecute = Mockito.mock(IKeyStoreSafeExecute::class.java)
    private val interactor = GuestInteractor(wordsManager, adapterManager, keystoreSafeExecute)

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
    fun createWallet_success() {
        val words = listOf("1", "2", "etc")

        whenever(wordsManager.createWords()).thenReturn(words)

        interactor.createWallet()

        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue
        val successRunnable = successRunnableCaptor.firstValue

        actionRunnable.run()
        successRunnable.run()

        verify(adapterManager).initAdapters(words)
        verify(delegate).didCreateWallet()
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun createWallet_error() {

        interactor.createWallet()

        verify(keystoreSafeExecute).safeExecute(actionRunnableCaptor.capture(), successRunnableCaptor.capture(), failureRunnableCaptor.capture())

        val actionRunnable = actionRunnableCaptor.firstValue
        val failureRunnable = failureRunnableCaptor.firstValue

        actionRunnable.run()
        failureRunnable.run()

        verify(delegate).didFailToCreateWallet()
        verifyNoMoreInteractions(delegate)
    }

}