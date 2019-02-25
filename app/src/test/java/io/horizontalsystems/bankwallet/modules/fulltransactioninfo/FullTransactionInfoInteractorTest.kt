package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.managers.TransactionDataProviderManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class FullTransactionInfoInteractorTest {

    private val delegate = mock(FullTransactionInfoModule.InteractorDelegate::class.java)
    private val transactionRecord = mock(FullTransactionRecord::class.java)
    private val transactionProvider = mock(FullTransactionInfoModule.FullProvider::class.java)

    private val transactionInfoFactory = mock(FullTransactionInfoFactory::class.java)
    private val dataProviderManager = mock(TransactionDataProviderManager::class.java)
    private val clipboardManager = mock(TextHelper::class.java)

    private val transactionHash = "abc"
    private val coin = mock(Coin::class.java)

    private lateinit var interactor: FullTransactionInfoInteractor

    @Before
    fun setup() {
        RxBaseTest.setup()

        whenever(coin.type).thenReturn(mock(CoinType.Bitcoin::class.java))

        whenever(transactionProvider.retrieveTransactionInfo(any()))
                .thenReturn(Flowable.empty())

        whenever(transactionInfoFactory.providerFor(coin))
                .thenReturn(transactionProvider)

        interactor = FullTransactionInfoInteractor(transactionInfoFactory, dataProviderManager, clipboardManager)
        interactor.delegate = delegate
    }

    @Test
    fun updateProvider() {
        interactor.updateProvider(coin)

        verify(transactionInfoFactory).providerFor(coin)
    }

    @Test
    fun url() {
        interactor.updateProvider(coin)
        interactor.url(transactionHash)

        verify(transactionProvider).url(transactionHash)
    }

    @Test
    fun retrieveTransactionInfo() {
        interactor.updateProvider(coin)
        interactor.retrieveTransactionInfo(transactionHash)

        verify(transactionProvider).retrieveTransactionInfo(transactionHash)
    }

    @Test
    fun onReceiveTransactionInfo() {
        interactor.onReceiveTransactionInfo(transactionRecord)

        verify(interactor.delegate!!).onReceiveTransactionInfo(transactionRecord)
    }

}
