//package io.horizontalsystems.bankwallet.modules.transactionInfo
//
//import com.nhaarman.mockito_kotlin.any
//import com.nhaarman.mockito_kotlin.verify
//import io.horizontalsystems.bankwallet.core.IClipboardManager
//import io.horizontalsystems.bankwallet.core.storage.AppDatabase
//import io.horizontalsystems.bankwallet.modules.RxBaseTest
//import org.junit.Before
//import org.junit.Test
//import org.mockito.Mockito
//
//class TransactionInfoInteractorTest {
//
//    private val delegate = Mockito.mock(TransactionInfoModule.IInteractorDelegate::class.java)
//    private val clipboardManager = Mockito.mock(IClipboardManager::class.java)
//    private val database = Mockito.mock(AppDatabase::class.java)
////    private val currencyUsd = Currency(code = "USD", symbol = "\u0024")
//
////    private val transaction = TransactionRecordViewItem(
////            "",
////            "",
////            CoinValue(Bitcoin(), 0.0),
////            CoinValue(Bitcoin(), 0.0),
////            "",
////            "",
////            true,
////            0,
////            Date(),
////            TransactionStatus.Completed,
////            CurrencyValue(currencyUsd, 0.0),
////            0.0
////    )
//
//    private val interactor = TransactionInfoInteractor(database, clipboardManager)
//
//    @Before
//    fun setUp() {
//        RxBaseTest.setup()
//
//        interactor.delegate = delegate
//    }
//
//    @Test
//    fun getTransaction() {
//        val transHash = "0123"
//        interactor.getTransaction(transHash)
//        verify(delegate).didGetTransaction(any())
//    }
//
//    @Test
//    fun onCopy() {
//        val value = "value"
//        interactor.onCopy(value)
//        verify(clipboardManager).copyText(value)
//    }
//
////    @Test
////    fun showFullInfo() {
////        interactor.showFullInfo()
////        verify(delegate).showFullInfo(any())
////    }
//
//}
