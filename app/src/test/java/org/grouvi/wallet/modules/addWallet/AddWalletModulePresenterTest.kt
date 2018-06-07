package org.grouvi.wallet.modules.addWallet

import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class AddWalletModulePresenterTest {

    private val presenter = AddWalletModulePresenter()
    private val interactor = mock(AddWalletModule.IInteractor::class.java)
    private val router = mock(AddWalletModule.IRouter::class.java)

    @Before
    fun before() {
        presenter.interactor = interactor
        presenter.router = router
    }

    @Test
    fun createWallet() {
        presenter.createWallet()

        verify(interactor).createWallet()
    }

    @Test
    fun didCreateWallet() {
        presenter.didCreateWallet()

        verify(router).openBackupScreen()
    }

}