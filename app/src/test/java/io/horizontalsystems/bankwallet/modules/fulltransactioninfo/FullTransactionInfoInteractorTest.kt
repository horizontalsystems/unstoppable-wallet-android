package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class FullTransactionInfoInteractorTest {

    private val delegate = mock(FullTransactionInfoModule.InteractorDelegate::class.java)
    private val transactionRecord = mock(FullTransactionRecord::class.java)
    private val transactionProvider = mock(FullTransactionInfoModule.Provider::class.java)
    private val transactionHash = "abc"

    private lateinit var interactor: FullTransactionInfoInteractor

    @Before
    fun setup() {
        RxBaseTest.setup()

        whenever(transactionProvider.retrieveTransactionInfo(any()))
                .thenReturn(Flowable.empty())

        interactor = FullTransactionInfoInteractor(transactionProvider)
        interactor.delegate = delegate
    }

    @Test
    fun retrieveTransactionInfo() {
        interactor.retrieveTransactionInfo(transactionHash)

        verify(transactionProvider).retrieveTransactionInfo(transactionHash)
    }

    @Test
    fun onReceiveTransactionInfo() {
        interactor.onReceiveTransactionInfo(transactionRecord)

        verify(interactor.delegate!!).onReceiveTransactionInfo(transactionRecord)
    }

//    @Test
//    fun retrieveTransaction() {
//        whenever(bitcoinAdapter.transactionRecords).thenReturn(listOf(transaction))
//        whenever(bitcoinAdapter.transactionRecordsSubject).thenReturn(PublishSubject.create())
//        whenever(bitcoinAdapter.coin).thenReturn(coin)
//
//        interactor.retrieveTransaction()
//
//        verify(delegate, atLeastOnce()).didGetTransactionInfo(any())
//    }
//
//    @Test
//    fun getTransactionInfo() {
//        interactor.retrieveTransaction()
//
//        interactor.getTransactionInfo()
//        verify(delegate, atLeast(2)).didGetTransactionInfo(any())
//    }
//
//    @Test
//    fun onCopyFromAddress() {
//        interactor.retrieveTransaction()
//
//        interactor.onCopyFromAddress()
//        verify(clipboardManager).copyText(any())
//        verify(delegate).didCopyToClipboard()
//    }
//
//    @Test
//    fun onCopyToAddress() {
//        interactor.retrieveTransaction()
//
//        interactor.onCopyToAddress()
//        verify(clipboardManager).copyText(any())
//        verify(delegate).didCopyToClipboard()
//    }
//
//    @Test
//    fun onCopyTransactionId() {
//        interactor.retrieveTransaction()
//
//        interactor.onCopyTransactionId()
//        verify(clipboardManager).copyText(any())
//        verify(delegate).didCopyToClipboard()
//    }
//
//    @Test
//    fun showFullInfo() {
//        interactor.retrieveTransaction()
//
//        interactor.showBlockInfo()
//        verify(delegate).showBlockInfo(any())
//    }
//
//    @Test
//    fun openShareDialog() {
//        interactor.retrieveTransaction()
//        interactor.openShareDialog()
//        verify(delegate).openShareDialog(any())
//    }
//
//    @Test
//    fun transactionsUpdated() {
//        val subject: PublishSubject<Any> = PublishSubject.create()
//        whenever(bitcoinAdapter.transactionRecordsSubject).thenReturn(subject)
//
//        interactor.retrieveTransaction()
//        verify(delegate, atLeastOnce()).didGetTransactionInfo(any())
//
//        subject.onNext(Any())
//        verify(delegate, atLeastOnce()).didGetTransactionInfo(any())
//    }
//
}
