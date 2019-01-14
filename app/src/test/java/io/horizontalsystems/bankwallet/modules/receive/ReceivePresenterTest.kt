package io.horizontalsystems.bankwallet.modules.receive

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class ReceivePresenterTest {

    private val interactor = Mockito.mock(ReceiveModule.IInteractor::class.java)
    private val view = Mockito.mock(ReceiveModule.IView::class.java)

    private var coin = CoinCode()
    private val coinAddress = "[coin_address]"
    private val coinTitle = "[coin_address]"
    private val addressItem = AddressItem(address = coinAddress, coinCode = coin, coinTitle = coinTitle)
    private val addresses = listOf(addressItem)

    private lateinit var presenter: ReceivePresenter

    @Before
    fun setUp() {
        presenter = ReceivePresenter(interactor)
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {

        presenter.viewDidLoad()

        verify(interactor).getReceiveAddress()
    }

    @Test
    fun didReceiveAddress() {
        presenter.didReceiveAddresses(addresses)

        verify(view).showAddresses(addresses)
    }

    @Test
    fun didFailToReceiveAddress() {
        presenter.didFailToReceiveAddress(Exception())

        verify(view).showError(R.string.Error)
    }

    @Test
    fun onCopyClick() {
        presenter.didReceiveAddresses(addresses)
        presenter.onCopyClick(any())

        verify(interactor).copyToClipboard(addressItem.address)
    }

    @Test
    fun didCopyToClipboard() {
        presenter.didCopyToClipboard()

        verify(view).showCopied()
    }

}
