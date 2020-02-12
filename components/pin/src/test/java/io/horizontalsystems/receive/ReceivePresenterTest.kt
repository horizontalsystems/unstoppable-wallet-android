package io.horizontalsystems.bankwallet.modules.receive

import com.nhaarman.mockito_kotlin.verify
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class ReceivePresenterTest {

    private val interactor = Mockito.mock(ReceiveModule.IInteractor::class.java)
    private val router = Mockito.mock(ReceiveModule.IRouter::class.java)
    private val view = Mockito.mock(ReceiveModule.IView::class.java)

    private var coin = Mockito.mock(Coin::class.java)
    private val coinAddress = "[coin_address]"
    private val addressItem = AddressItem(address = coinAddress, addressType = null, coin = coin)

    private lateinit var presenter: ReceivePresenter

    @Before
    fun setUp() {
        presenter = ReceivePresenter(view, router, interactor)
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()

        verify(interactor).getReceiveAddress()
    }

    @Test
    fun didReceiveAddress() {
        presenter.didReceiveAddress(addressItem)

        verify(view).showAddress(addressItem)
    }

    @Test
    fun didFailToReceiveAddress() {
        presenter.didFailToReceiveAddress(Exception())

        verify(view).showError(R.string.Error)
    }

    @Test
    fun onShareClick() {
        presenter.didReceiveAddress(addressItem)
        presenter.onShareClick()

        verify(router).shareAddress(addressItem.address)
    }

    @Test
    fun onAddressClick() {
        presenter.didReceiveAddress(addressItem)
        presenter.onAddressClick()

        verify(interactor).copyToClipboard(addressItem.address)
    }

    @Test
    fun didCopyToClipboard() {
        presenter.didCopyToClipboard()

        verify(view).showCopied()
    }

}
