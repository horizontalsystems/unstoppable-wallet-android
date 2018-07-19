package bitcoin.wallet.modules.receive

import bitcoin.wallet.blockchain.BlockchainManager
import bitcoin.wallet.core.IClipboardManager
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class ReceiveInteractorTest {

    private val delegate = mock(ReceiveModule.IInteractorDelegate::class.java)
    private val blockchainManager = mock(BlockchainManager::class.java)
    private val clipboardManager = mock(IClipboardManager::class.java)

    private val interactor = ReceiveInteractor(blockchainManager, clipboardManager)

    @Before
    fun setUp() {
        interactor.delegate = delegate
    }

    @Test
    fun getReceiveAddress() {
        val coinCode = "BTC"
        interactor.getReceiveAddress(coinCode)

        verify(blockchainManager).getReceiveAddress(coinCode)
    }

    @Test
    fun didReceiveAddress() {
        val coinCode = "BTC"
        val coinAddress = "[coin_address]"

        whenever(blockchainManager.getReceiveAddress(coinCode)).thenReturn(coinAddress)
        interactor.getReceiveAddress(coinCode)

        verify(delegate).didReceiveAddress(coinAddress)
    }

    @Test
    fun failedReceiveAddress() {
        val coinCode = ""
        val exception = Exception("")
        whenever(blockchainManager.getReceiveAddress(coinCode)).thenThrow(exception)

        interactor.getReceiveAddress("")

        verify(delegate).didFailToReceiveAddress(exception)
    }

    @Test
    fun copyToClipboard() {
        val address = "[coin_address]"

        interactor.copyToClipboard(address)

        verify(clipboardManager).copyText(address)
    }

    @Test
    fun didCopyToClipboard() {

        val address = "[coin_address]"

        interactor.copyToClipboard(address)

        verify(delegate).didCopyToClipboard()
    }

}
