package bitcoin.wallet.modules.restore

import android.security.keystore.UserNotAuthenticatedException
import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.managers.WordsManager
import bitcoin.wallet.kit.hdwallet.Mnemonic
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

class RestoreInteractorTest {

    private val wordsManager = Mockito.mock(WordsManager::class.java)
    private val delegate = Mockito.mock(RestoreModule.IInteractorDelegate::class.java)
    private val adapterManager = Mockito.mock(AdapterManager::class.java)
    private val interactor = RestoreInteractor(wordsManager, adapterManager)


    @Before
    fun before() {
        interactor.delegate = delegate
    }

    @Test
    fun restoreWallet_restore() {
        val words = listOf("first", "second", "etc")

        interactor.restore(words)

        verify(wordsManager).restore(words)
    }

    @Test
    fun restoreWallet_success() {
        val words = listOf("first", "second", "etc")

        interactor.restore(words)

        verify(delegate).didRestore()
        verify(wordsManager).wordListBackedUp = true
        verify(adapterManager).initAdapters(any())
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun restoreWallet_failureWordsError() {
        val words = listOf("first", "second", "etc")
        val exception = Mnemonic.MnemonicException("Invalid words")

        whenever(wordsManager.restore(words)).thenThrow(exception)

        interactor.restore(words)

        verify(delegate).didFailToRestore(exception)
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun restoreWallet_userNotAuthenticatedFailure() {
        val words = listOf("first", "second", "etc")
        val exception = UserNotAuthenticatedException()

        whenever(wordsManager.restore(words)).thenThrow(exception)

        interactor.restore(words)

        verify(delegate).didFailToRestore(exception)
        verifyNoMoreInteractions(delegate)
    }
}
