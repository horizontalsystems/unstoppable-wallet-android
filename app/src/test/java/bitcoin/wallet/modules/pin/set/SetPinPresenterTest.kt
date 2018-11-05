package bitcoin.wallet.modules.pin.set

import bitcoin.wallet.modules.RxBaseTest
import bitcoin.wallet.modules.pin.PinModule
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class SetPinPresenterTest {

    private val interactor = Mockito.mock(PinModule.IPinInteractor::class.java)
    private val router = Mockito.mock(SetPinModule.ISetPinRouter::class.java)
    private val view = Mockito.mock(PinModule.IPinView::class.java)
    private var presenter = SetPinPresenter(interactor, router)

    @Before
    fun setUp() {
        RxBaseTest.setup()
        presenter.view = view
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
        verify(router).navigateToMain()
    }
}