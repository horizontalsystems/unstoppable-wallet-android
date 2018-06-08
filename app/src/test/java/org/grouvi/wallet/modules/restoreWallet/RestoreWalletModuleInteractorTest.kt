package org.grouvi.wallet.modules.restoreWallet

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class RestoreWalletModuleInteractorTest {

    private var interactor = RestoreWalletModuleInteractor()
    private var delegate = mock(RestoreWalletModule.IInteractorDelegate::class.java)
    private var walletRestorer = mock(RestoreWalletModule.IWalletRestorer::class.java)

    @Before
    fun before() {
        interactor.walletRestorer = walletRestorer
        interactor.delegate = delegate
    }

    @Test
    fun restoreWallet() {
        val words = listOf("first", "second", "etc")

        interactor.restoreWallet(words)

        verify(walletRestorer).restoreWallet(words)
    }

    @Test
    fun restoreWallet_success() {
        val words = listOf("first", "second", "etc")

        interactor.restoreWallet(words)

        verify(delegate).didRestoreWallet()
        verifyNoMoreInteractions(delegate)
    }

    @Test
    fun restoreWallet_failure() {
        val words = listOf("first", "second", "etc")

        val exception = RestoreWalletModule.InvalidWordsException()

        whenever(walletRestorer.restoreWallet(words)).thenThrow(exception)

        interactor.restoreWallet(words)

        verify(delegate).failureRestoreWallet(exception)
        verifyNoMoreInteractions(delegate)
    }
}