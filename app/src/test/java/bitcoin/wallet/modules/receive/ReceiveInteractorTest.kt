package bitcoin.wallet.modules.receive

import bitcoin.wallet.core.managers.AdapterManager
import bitcoin.wallet.core.BitcoinAdapter
import bitcoin.wallet.core.IClipboardManager
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.modules.receive.viewitems.AddressItem
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class ReceiveInteractorTest {

    private val delegate = mock(ReceiveModule.IInteractorDelegate::class.java)
    private val clipboardManager = mock(IClipboardManager::class.java)
    private val adapterManager = mock(AdapterManager::class.java)
    private val bitcoinAdapter = mock(BitcoinAdapter::class.java)

    private var coin = Bitcoin()
    private var words = listOf("used", "ugly", "meat", "glad", "balance", "divorce", "inner", "artwork", "hire", "invest", "already", "piano")
    private var wordsHash = words.joinToString(" ")
    private var adapterId: String = "${wordsHash.hashCode()}-${coin.code}"
    private var interactor = ReceiveInteractor(adapterManager, adapterId, clipboardManager)

    @Before
    fun setUp() {
        interactor.delegate = delegate
    }

    @Test
    fun getReceiveAddress_noSimilarAdapter() {
        val otherAdapterId = "[other_adapter_id]"
        interactor = ReceiveInteractor(adapterManager, otherAdapterId, clipboardManager)

        interactor.getReceiveAddress()

        verify(delegate, never()).didReceiveAddresses(any())
    }

    @Test
    fun didReceiveAddress() {
        val coinAddress = "[coin_address]"

        val addresses = mutableListOf<AddressItem>()
        val addressItem = AddressItem(adapterId = adapterId, address = coinAddress, coin = coin)
        addresses.add(addressItem)

        whenever(adapterManager.adapters).thenReturn(mutableListOf(bitcoinAdapter))
        whenever(bitcoinAdapter.id).thenReturn(adapterId)
        whenever(bitcoinAdapter.coin).thenReturn(coin)
        whenever(bitcoinAdapter.receiveAddress).thenReturn(coinAddress)

        interactor.getReceiveAddress()

        verify(delegate).didReceiveAddresses(addresses)
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
