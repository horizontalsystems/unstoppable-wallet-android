package io.horizontalsystems.bankwallet.modules.receive

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class ReceiveInteractorTest {
    private val clipboardManager = mock(IClipboardManager::class.java)
    private var walletManager = mock(IWalletManager::class.java)
    private var wallet = mock(Wallet::class.java)
    private val adapter = mock(IAdapter::class.java)
    private val delegate = mock(ReceiveModule.IInteractorDelegate::class.java)

    private lateinit var coin: CoinCode
    private lateinit var interactor: ReceiveInteractor

    private val coinAddress = "[coin_address]"

    @Before
    fun setup() {
        coin = CoinCode()
        interactor = ReceiveInteractor(coin, walletManager, clipboardManager)
        interactor.delegate = delegate
    }

    @Test
    fun didReceiveAddress() {
        val coinTitle = "Bitcoin"

        whenever(adapter.receiveAddress).thenReturn(coinAddress)
        whenever(wallet.coinCode).thenReturn(coin)
        whenever(wallet.adapter).thenReturn(adapter)
        whenever(wallet.title).thenReturn(coinTitle)
        whenever(walletManager.wallets).thenReturn(listOf(wallet))

        interactor.getReceiveAddress()

        verify(delegate).didReceiveAddresses(listOf(AddressItem(coinAddress, coin, coinTitle)))
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
