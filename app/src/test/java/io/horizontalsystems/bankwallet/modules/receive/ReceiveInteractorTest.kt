package io.horizontalsystems.bankwallet.modules.receive

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class ReceiveInteractorTest {
    private val clipboardManager = mock(IClipboardManager::class.java)
    private var adapterManager = mock(IAdapterManager::class.java)
    private val adapter = mock(IAdapter::class.java)
    private val delegate = mock(ReceiveModule.IInteractorDelegate::class.java)

    private val coinTitle = "Bitcoin"
    private val coinCode: CoinCode = "coinCode"
    private val coin = mock(Coin::class.java)
    private lateinit var interactor: ReceiveInteractor

    private val coinAddress = "[coin_address]"

    @Before
    fun setup() {
        whenever(coin.code).thenReturn(coinCode)
        whenever(coin.title).thenReturn(coinTitle)

        interactor = ReceiveInteractor(coinCode, adapterManager, clipboardManager)
        interactor.delegate = delegate
    }

    @Test
    fun didReceiveAddress() {
        whenever(adapter.wallet).thenReturn(coin)
        whenever(adapter.receiveAddress).thenReturn(coinAddress)
        whenever(adapterManager.adapters).thenReturn(listOf(adapter))

        interactor.getReceiveAddress()

        verify(delegate).didReceiveAddress(AddressItem(coinAddress, coin))
    }

    @Test
    fun copyToClipboard() {
        interactor.copyToClipboard(coinAddress)

        verify(clipboardManager).copyText(coinAddress)
    }

    @Test
    fun didCopyToClipboard() {
        interactor.copyToClipboard(coinAddress)

        verify(delegate).didCopyToClipboard()
    }

}
