package bitcoin.wallet.modules.receive

import bitcoin.wallet.R
import bitcoin.wallet.blockchain.UnsupportedBlockchain
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class ReceivePresenterTest {

    private val interactor = Mockito.mock(ReceiveModule.IInteractor::class.java)
    private val router = Mockito.mock(ReceiveModule.IRouter::class.java)
    private val view = Mockito.mock(ReceiveModule.IView::class.java)

    private val coinCode = "BTC"

    private val presenter = ReceivePresenter(interactor, router, coinCode)

    @Before
    fun setUp() {
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {

        presenter.viewDidLoad()

        verify(interactor).getReceiveAddress(coinCode)
    }

    @Test
    fun didReceiveAddress() {
        val address = "[coin_address]"

        presenter.didReceiveAddress(address)

        verify(view).showAddress(address)
    }

    @Test
    fun didFailToReceiveAddress_general() {
        val exception = Exception()

        presenter.didFailToReceiveAddress(exception)

        verify(view).showError(R.string.error)
    }

    @Test
    fun didFailToReceiveAddress_unsupportedBlockchain() {
        val exception = UnsupportedBlockchain("ETH")

        presenter.didFailToReceiveAddress(exception)

        verify(view).showError(R.string.error_unsupported_blockchain)
    }

    @Test
    fun onCopyClick() {
        val address = "[coin_address]"

        presenter.didReceiveAddress(address)
        presenter.onCopyClick()

        verify(interactor).copyToClipboard(address)
    }

    @Test
    fun didCopyToClipboard() {
        presenter.didCopyToClipboard()

        verify(view).showCopied()
    }

    @Test
    fun onShareClick() {
        val address = "[coin_address]"

        presenter.didReceiveAddress(address)
        presenter.onShareClick()

        verify(router).openShareView(address)
    }

}
