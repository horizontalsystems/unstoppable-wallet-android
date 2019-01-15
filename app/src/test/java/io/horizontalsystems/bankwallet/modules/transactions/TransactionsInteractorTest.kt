package io.horizontalsystems.bankwallet.modules.transactions

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class TransactionsInteractorTest {

    private val walletManager = mock(IWalletManager::class.java)
    private val delegate = mock(TransactionsModule.IInteractorDelegate::class.java)

    private val wallet1 = mock(Wallet::class.java)
    private val wallet2 = mock(Wallet::class.java)
    private val adapter1 = mock(IAdapter::class.java)
    private val adapter2 = mock(IAdapter::class.java)
    private val blockHeightSubject = PublishSubject.create<Int>()
    private val walletsUpdatedSignal = PublishSubject.create<Unit>()

    private lateinit var interactor: TransactionsInteractor

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor = TransactionsInteractor(walletManager)
        interactor.delegate = delegate

        whenever(walletManager.walletsUpdatedSignal).thenReturn(walletsUpdatedSignal)
    }

    @Test
    fun fetchLastBlockHeights() {
        val lastBlockHeight = 12312
        val confirmationThreshold = 12312
        val adapter1 = mock(IAdapter::class.java)
        val wallets = listOf(wallet1)
        val coinCode1 = "BTC"

        val mockSubject = mock<PublishSubject<Unit>>()
        whenever(mockSubject.throttleLast(any(), any())).thenReturn(PublishSubject.create())

        whenever(adapter1.lastBlockHeightUpdatedSignal).thenReturn(mockSubject)
        whenever(adapter1.lastBlockHeight).thenReturn(lastBlockHeight)
        whenever(adapter1.confirmationsThreshold).thenReturn(confirmationThreshold)
        whenever(wallet1.adapter).thenReturn(adapter1)
        whenever(wallet1.coinCode).thenReturn(coinCode1)
        whenever(walletManager.wallets).thenReturn(wallets)

        interactor.fetchLastBlockHeights()

        verify(delegate).onUpdateLastBlockHeight(coinCode1, lastBlockHeight)
        verify(delegate).onUpdateConfirmationThreshold(coinCode1, confirmationThreshold)
    }

    @Test
    fun fetchLastBlockHeights_update() {
        val lastBlockHeightUpdatedSignal1 = PublishSubject.create<Unit>()
        val lastBlockHeight = 12312
        val lastBlockHeightUpdated = 345345
        val adapter1 = mock(IAdapter::class.java)
        val wallets = listOf(wallet1)
        val coinCode1 = "BTC"

        val mockSubject = mock<PublishSubject<Unit>>()
        whenever(mockSubject.throttleLast(any(), any())).thenReturn(lastBlockHeightUpdatedSignal1)

        whenever(adapter1.lastBlockHeightUpdatedSignal).thenReturn(mockSubject)
        whenever(adapter1.lastBlockHeight).thenReturn(lastBlockHeight, lastBlockHeightUpdated)
        whenever(wallet1.adapter).thenReturn(adapter1)
        whenever(wallet1.coinCode).thenReturn(coinCode1)
        whenever(walletManager.wallets).thenReturn(wallets)

        interactor.fetchLastBlockHeights()

        lastBlockHeightUpdatedSignal1.onNext(Unit)

        verify(delegate).onUpdateLastBlockHeight(coinCode1, lastBlockHeight)
        verify(delegate).onUpdateLastBlockHeight(coinCode1, lastBlockHeightUpdated)
    }

    @Test
    fun fetchCoinCodes() {
        val wallets = listOf(wallet1)
        val allCoinCodes = listOf("BTC")

        whenever(wallet1.coinCode).thenReturn("BTC")
        whenever(walletManager.wallets).thenReturn(wallets)

        interactor.fetchCoinCodes()

        verify(delegate).onUpdateCoinCodes(allCoinCodes)
    }

    @Test
    fun fetchCoinCodes_walletsUpdated() {
        val wallets: List<Wallet> = listOf()
        val walletUpdated: List<Wallet> = listOf(wallet1)
        val allCoinCodes = listOf("BTC")

        whenever(wallet1.coinCode).thenReturn("BTC")
        whenever(walletManager.wallets).thenReturn(wallets, walletUpdated)

        interactor.fetchCoinCodes()

        walletsUpdatedSignal.onNext(Unit)

        verify(delegate).onUpdateCoinCodes(listOf())
        verify(delegate).onUpdateCoinCodes(allCoinCodes)
    }

    @Test
    fun setSelectedCoinCodes() {
        interactor.setSelectedCoinCodes(listOf("BTC"))

        verify(delegate).onUpdateSelectedCoinCodes(listOf("BTC"))
    }

    @Test
    fun setSelectedCoinCodes_empty() {
        val wallets = listOf(wallet1, wallet2)
        val allCoinCodes = listOf("BTC", "ETH")

        whenever(wallet1.coinCode).thenReturn("BTC")
        whenever(wallet2.coinCode).thenReturn("ETH")
        whenever(walletManager.wallets).thenReturn(wallets)

        interactor.setSelectedCoinCodes(listOf())

        verify(delegate).onUpdateSelectedCoinCodes(allCoinCodes)
    }

    @Test
    fun fetchItems() {
        val hashFrom1 = null
        val hashFrom2 = "hashFrom2"

        val limit1 = 5
        val limit2 = 12

        val fetchDataList = listOf(
                TransactionsModule.FetchData("BTC", hashFrom1, limit1),
                TransactionsModule.FetchData("ETH", hashFrom2, limit2),
                TransactionsModule.FetchData("BCH", null, 17)
        )
        val transactionRecords1 = listOf<TransactionRecord>(mock(TransactionRecord::class.java))
        val transactionRecords2 = listOf<TransactionRecord>(mock(TransactionRecord::class.java))

        whenever(walletManager.wallets).thenReturn(listOf(wallet1, wallet2))
        whenever(wallet1.coinCode).thenReturn("BTC")
        whenever(wallet2.coinCode).thenReturn("ETH")
        whenever(wallet1.adapter).thenReturn(adapter1)
        whenever(wallet2.adapter).thenReturn(adapter2)
        whenever(adapter1.getTransactionsObservable(hashFrom1, limit1)).thenReturn(Flowable.just(transactionRecords1))
        whenever(adapter2.getTransactionsObservable(hashFrom2, limit2)).thenReturn(Flowable.just(transactionRecords2))

        interactor.fetchRecords(fetchDataList)

        val records = mapOf("BTC" to transactionRecords1, "ETH" to transactionRecords2, "BCH" to listOf())

        verify(delegate).didFetchRecords(records)
    }

}
