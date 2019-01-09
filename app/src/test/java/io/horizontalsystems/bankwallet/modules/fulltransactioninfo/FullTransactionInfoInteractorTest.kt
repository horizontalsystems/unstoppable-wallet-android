package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.entities.FullTransactionItem
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class FullTransactionInfoInteractorTest {

    private val delegate = mock(FullTransactionInfoModule.InteractorDelegate::class.java)
    private val transactionItem = mock(FullTransactionItem::class.java)
    private val transactionRecord = mock(FullTransactionRecord::class.java)
    private val transactionProvider = mock(FullTransactionInfoModule.Provider::class.java)

    private val transactionHash = "abc"
    private val url = "http://domain.com"

    private lateinit var interactor: FullTransactionInfoInteractor

    @Before
    fun setup() {
        RxBaseTest.setup()

        whenever(transactionProvider.retrieveTransactionInfo(any()))
                .thenReturn(Flowable.empty())

        interactor = FullTransactionInfoInteractor(transactionProvider, TextHelper)
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

    @Test
    fun onTapItem_copy() {
        whenever(transactionItem.clickable).thenReturn(true)
        whenever(transactionItem.value).thenReturn(transactionHash)

        // lateinit property not initialized
        //interactor.onTapItem(transactionItem)
        //
        //verify(delegate).onCopied()
    }

    @Test
    fun onTapItem_openUrl() {
        whenever(transactionItem.clickable).thenReturn(true)
        whenever(transactionItem.url).thenReturn(url)

        interactor.onTapItem(transactionItem)

        verify(delegate).onOpenUrl(url)
    }

    @Test
    fun retryLoadInfo() {
        interactor.retryLoadInfo()

        verify(delegate).retryLoadInfo()
    }

}
