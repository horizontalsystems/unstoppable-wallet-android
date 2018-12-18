package io.horizontalsystems.bankwallet.modules.transactionInfo

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.ITransactionRecordStorage
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.Maybe
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class TransactionInfoInteractorTest {

    private val delegate = mock(TransactionInfoModule.IInteractorDelegate::class.java)
    private val clipboardManager = mock(IClipboardManager::class.java)
    private val transactionRepository = mock(ITransactionRecordStorage::class.java)

    private lateinit var interactor: TransactionInfoInteractor

    private val transHash = "0123"

    @Before
    fun setup() {
        RxBaseTest.setup()

        val record = Maybe.just<TransactionRecord>(TransactionRecord())
        whenever(transactionRepository.record(transHash)).thenReturn(record)

        interactor = TransactionInfoInteractor(transactionRepository, clipboardManager)
        interactor.delegate = delegate
    }

    @Test
    fun getTransaction() {
        interactor.getTransaction(transHash)
        verify(delegate).didGetTransaction(any())
    }

    @Test
    fun onCopy() {
        val value = "value"
        interactor.onCopy(value)
        verify(clipboardManager).copyText(value)
    }

//    @Test
//    fun showFullInfo() {
//        interactor.showFullInfo()
//        verify(delegate).showFullInfo(any())
//    }

}
