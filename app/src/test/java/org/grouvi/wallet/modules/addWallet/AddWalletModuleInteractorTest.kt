package org.grouvi.wallet.modules.addWallet

import com.nhaarman.mockito_kotlin.verify
import org.grouvi.wallet.lib.WalletDataManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class AddWalletModuleInteractorTest {

    private val interactor = AddWalletModuleInteractor()
    private val walletDataProvider = mock(WalletDataManager::class.java)
    private val delegate = mock(AddWalletModule.IInteractorDelegate::class.java)

    @Before
    fun before() {
        interactor.delegate = delegate
        interactor.walletDataProvider = walletDataProvider
    }

    @Test
    fun createWallet() {
        interactor.createWallet()

        verify(walletDataProvider).createWallet()
        verify(delegate).didCreateWallet()
    }
}