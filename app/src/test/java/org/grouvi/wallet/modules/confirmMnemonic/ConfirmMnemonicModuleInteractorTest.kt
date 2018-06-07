package org.grouvi.wallet.modules.confirmMnemonic

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.grouvi.wallet.lib.WalletDataManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.*

class ConfirmMnemonicModuleInteractorTest {

    private val interactor = ConfirmMnemonicModuleInteractor()
    private val delegate = mock(ConfirmMnemonicModule.IInteractorDelegate::class.java)
    private val walletDataProvider = mock(WalletDataManager::class.java)
    private val random = mock(Random::class.java)

    @Before
    fun before() {
        interactor.delegate = delegate
        interactor.walletDataProvider = walletDataProvider
        interactor.random = random
    }

    @Test
    fun retrieveWordForConfirm() {

        val wordsCount = 12

        val words = mock<List<String>>()

        whenever(words.size).thenReturn(wordsCount)
        whenever(walletDataProvider.mnemonicWords).thenReturn(words)
        whenever(random.nextInt(wordsCount)).thenReturn(6)

        interactor.retrieveConfirmationWord()

        verify(delegate).didConfirmationWordRetrieve(6)
    }

    @Test
    fun validateConfirmationWord_success() {
        val word = "despacito"
        val position = 10
        val mnemonicWords = mock<List<String>>()

        whenever(walletDataProvider.mnemonicWords).thenReturn(mnemonicWords)
        whenever(mnemonicWords[position]).thenReturn(word)

        interactor.validateConfirmationWord(position, word)

        verify(delegate).didConfirmationSuccess()
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun validateConfirmationWord_error() {
        val position = 10
        val mnemonicWords = mock<List<String>>()

        whenever(walletDataProvider.mnemonicWords).thenReturn(mnemonicWords)
        whenever(mnemonicWords[position]).thenReturn("danzakuduro")

        interactor.validateConfirmationWord(position, "despacito")

        verify(delegate).didConfirmationFailure()
        verifyNoMoreInteractions(delegate)
    }
}