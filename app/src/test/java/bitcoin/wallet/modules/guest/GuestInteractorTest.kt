package bitcoin.wallet.modules.guest

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.core.managers.WordsManager
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class GuestInteractorTest {

    private val wordsManager = mock(WordsManager::class.java)
    private val factory = mock(Factory::class.java)
    private val delegate = mock(GuestModule.IInteractorDelegate::class.java)
    private val adapterManager = mock(AdapterManager::class.java)
    private val interactor = GuestInteractor(wordsManager, adapterManager)

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor.delegate = delegate
        whenever(factory.wordsManager).thenReturn(wordsManager)
    }

    @Test
    fun createWallet_success() {
        val words = listOf("1", "2", "etc")

        whenever(wordsManager.createWords()).thenReturn(words)

        interactor.createWallet()

        verify(adapterManager).initAdapters(words)
        verify(delegate).didCreateWallet()
        verifyNoMoreInteractions(delegate)
    }

}