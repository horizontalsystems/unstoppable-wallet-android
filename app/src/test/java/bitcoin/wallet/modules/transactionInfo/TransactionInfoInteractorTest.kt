package bitcoin.wallet.modules.transactionInfo

import bitcoin.wallet.core.IClipboardManager
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.TransactionStatus
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.modules.RxBaseTest
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.*

class TransactionInfoInteractorTest {

    private val delegate = Mockito.mock(TransactionInfoModule.IInteractorDelegate::class.java)
    private val clipboardManager = Mockito.mock(IClipboardManager::class.java)

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
            CurrencyValue(Currency(), 0.0),
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
    fun onCopyFromAddress() {
        interactor.onCopyFromAddress()
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
