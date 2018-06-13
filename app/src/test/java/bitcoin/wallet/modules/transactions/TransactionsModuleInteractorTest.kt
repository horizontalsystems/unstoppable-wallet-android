package bitcoin.wallet.modules.transactions

import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Flowable
import junit.framework.Assert.assertEquals
import bitcoin.wallet.entities.Transaction
import bitcoin.wallet.entities.TransactionInput
import bitcoin.wallet.entities.TransactionOutput
import bitcoin.wallet.modules.RxBaseTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.util.*

class TransactionsModuleInteractorTest {

    private val interactor = TransactionsModuleInteractor()
    private val transactionsDataProvider = mock(ITransactionsDataProvider::class.java)
    private val addressesProvider = mock(IAddressesProvider::class.java)
    private val delegate = mock(TransactionsModule.IInteractorDelegate::class.java)

    private val myAddresses = listOf("mine", "mine-change")

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor.delegate = delegate
        interactor.transactionsDataProvider = transactionsDataProvider
        interactor.addressesProvider = addressesProvider

        whenever(addressesProvider.getAddresses()).thenReturn(myAddresses)
    }

    @Test
    fun retrieveTransactionItems() {
        whenever(transactionsDataProvider.getTransactions()).thenReturn(Flowable.empty())

        interactor.retrieveTransactionItems()

        verify(transactionsDataProvider).getTransactions()
    }

    @Test
    fun retrieveTransactionItems_success() {
        val transactions = listOf<Transaction>()

        whenever(transactionsDataProvider.getTransactions()).thenReturn(Flowable.just(transactions))

        interactor.retrieveTransactionItems()

        verify(delegate).didTransactionItemsRetrieve(listOf())
    }


    @Test
    fun retrieveTransactionItems_transactionOutConvert() {
        val now = Date()

        val transactionOut = Transaction().apply {
            timestamp = now.time

            inputs = listOf(
                    TransactionInput().apply {
                        address = myAddresses.first()
                        value = 100000000
                    }
            )

            outputs = listOf(
                    TransactionOutput().apply {
                        address = "merchant"
                        value = 100000000
                    }
            )
        }

        val expectedTransactionViewItem = TransactionViewItem(TransactionViewItem.Type.OUT, 1.0, now)

        whenever(transactionsDataProvider.getTransactions()).thenReturn(Flowable.just(listOf(transactionOut)))

        interactor.retrieveTransactionItems()

        argumentCaptor<List<TransactionViewItem>>().apply {
            verify(delegate).didTransactionItemsRetrieve(capture())

            val actual = firstValue.first()

            assertEquals(expectedTransactionViewItem.type, actual.type)
            assertEquals(expectedTransactionViewItem.amount, actual.amount)
            assertEquals(expectedTransactionViewItem.date, actual.date)
        }

    }

    @Test
    fun retrieveTransactionItems_transactionOutWithChangeConvert() {
        val now = Date()

        val transactionOut = Transaction().apply {
            timestamp = now.time

            inputs = listOf(
                    TransactionInput().apply {
                        address = myAddresses[0]
                        value = 100000000
                    }
            )

            outputs = listOf(
                    TransactionOutput().apply {
                        address = "merchant"
                        value = 10000000
                    },
                    TransactionOutput().apply {
                        address = myAddresses[1]
                        value = 90000000
                    }
            )
        }

        val expectedTransactionViewItem = TransactionViewItem(TransactionViewItem.Type.OUT, 0.1, now)

        whenever(transactionsDataProvider.getTransactions()).thenReturn(Flowable.just(listOf(transactionOut)))

        interactor.retrieveTransactionItems()

        argumentCaptor<List<TransactionViewItem>>().apply {
            verify(delegate).didTransactionItemsRetrieve(capture())

            val actual = firstValue.first()

            assertEquals(expectedTransactionViewItem.type, actual.type)
            assertEquals(expectedTransactionViewItem.amount, actual.amount)
            assertEquals(expectedTransactionViewItem.date, actual.date)
        }

    }

    @Test
    fun retrieveTransactionItems_transactionInConvert() {
        val now = Date()

        val transactionIn = Transaction().apply {
            timestamp = now.time

            inputs = listOf(
                    TransactionInput().apply {
                        address = "work"
                        value = 100000000
                    }
            )

            outputs = listOf(
                    TransactionOutput().apply {
                        address = myAddresses.first()
                        value = 90000000
                    }
            )
        }

        val expectedTransactionViewItem = TransactionViewItem(TransactionViewItem.Type.IN, 0.9, now)

        whenever(transactionsDataProvider.getTransactions()).thenReturn(Flowable.just(listOf(transactionIn)))

        interactor.retrieveTransactionItems()

        argumentCaptor<List<TransactionViewItem>>().apply {
            verify(delegate).didTransactionItemsRetrieve(capture())

            val actual = firstValue.first()

            assertEquals(expectedTransactionViewItem.type, actual.type)
            assertEquals(expectedTransactionViewItem.amount, actual.amount)
            assertEquals(expectedTransactionViewItem.date, actual.date)
        }

    }
}