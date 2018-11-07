package io.horizontalsystems.bankwallet.modules.transactionInfo

import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.TransactionStatus
import io.horizontalsystems.bankwallet.entities.coins.bitcoin.Bitcoin
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.horizontalsystems.bankwallet.modules.transactions.TransactionRecordViewItem
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.*

class TransactionInfoInteractorTest {

    private val delegate = Mockito.mock(TransactionInfoModule.IInteractorDelegate::class.java)
    private val clipboardManager = Mockito.mock(IClipboardManager::class.java)
    private val currencyUsd = Currency(code = "USD", symbol = "\u0024")

    private val transaction = TransactionRecordViewItem(
            "",
            "",
            CoinValue(Bitcoin(), 0.0),
            CoinValue(Bitcoin(), 0.0),
            "",
            "",
            true,
            0,
            Date(),
            TransactionStatus.Completed,
            CurrencyValue(currencyUsd, 0.0),
            0.0
    )

    private val interactor = TransactionInfoInteractor(transaction, clipboardManager)

    @Before
    fun setUp() {
        RxBaseTest.setup()

        interactor.delegate = delegate
    }

    @Test
    fun getTransactionInfo() {
        interactor.getTransactionInfo()
        verify(delegate).didGetTransactionInfo(any())
    }

    @Test
    fun onCopyAddress() {
        interactor.onCopyAddress()
        verify(delegate).didCopyToClipboard()
    }

    @Test
    fun onCopyId() {
        interactor.onCopyId()
        verify(delegate).didCopyToClipboard()
    }

    @Test
    fun showFullInfo() {
        interactor.showFullInfo()
        verify(delegate).showFullInfo(any())
    }

}
