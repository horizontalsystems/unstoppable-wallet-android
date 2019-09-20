package io.horizontalsystems.bankwallet.modules.pin.set

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class SetPinPresenterTest {

    private val interactor = Mockito.mock(PinModule.IInteractor::class.java)
    private val router = Mockito.mock(SetPinModule.IRouter::class.java)
    private val view = Mockito.mock(PinModule.IView::class.java)
    private var presenter = SetPinPresenter(view, router, interactor)

    @Before
    fun setUp() {
        RxBaseTest.setup()
    }

    @Test
    fun viewDidLoad_setTitle() {
        presenter.viewDidLoad()
        verify(view).setTitle(any())
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()
        verify(view).addPages(any())
    }

    @Test
    fun showConfirm() {
        val pin = "000000"
        val confirmPageIndex = 1
        whenever(interactor.validate(any())).thenReturn(true)
        presenter.onEnter(pin, confirmPageIndex)

        verify(interactor).save(pin)
    }

    @Test
    fun didSavePin() {
        presenter.didSavePin()
        verify(router).dismissModuleWithSuccess()
    }
}
