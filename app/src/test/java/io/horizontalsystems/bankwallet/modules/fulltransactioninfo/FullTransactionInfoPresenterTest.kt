package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class FullTransactionInfoPresenterTest {

    private val interactor = mock(FullTransactionInfoInteractor::class.java)
    private val state = mock(FullTransactionInfoState::class.java)
    private val router = mock(FullTransactionInfoModule.Router::class.java)
    private val view = mock(FullTransactionInfoModule.View::class.java)
    private val transactionHash = "abc"
    private val transactionRecord = mock(FullTransactionRecord::class.java)

    private lateinit var presenter: FullTransactionInfoPresenter

    @Before
    fun setup() {
        whenever(state.transactionHash).thenReturn(transactionHash)

        presenter = FullTransactionInfoPresenter(interactor, router, state)
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {
        presenter.viewDidLoad()

        verify(view).showLoading()
        verify(interactor).retrieveTransactionInfo(transactionHash)
    }

    @Test
    fun onReceiveTransactionInfo() {
        presenter.onReceiveTransactionInfo(transactionRecord)

        verify(state).transactionRecord = transactionRecord
        verify(view).hideLoading()
        verify(view).reload()
    }

//
//    @Test
//    fun didGetTransactionInfo() {
//        presenter.didGetTransactionInfo(transactionRecordViewItem)
//        verify(view).showTransactionItem(any())
//    }
//
//    @Test
//    fun didCopyToClipboard() {
//        presenter.didCopyToClipboard()
//        verify(view).showCopied()
//    }
//
//    @Test
//    fun showBlockInfo() {
//        presenter.showBlockInfo(transactionRecordViewItem)
//        verify(router).showBlockInfo(any())
//    }
//
//    @Test
//    fun openShareDialog() {
//        presenter.openShareDialog(transactionRecordViewItem)
//        verify(router).shareTransaction(transactionRecordViewItem)
//    }
}
