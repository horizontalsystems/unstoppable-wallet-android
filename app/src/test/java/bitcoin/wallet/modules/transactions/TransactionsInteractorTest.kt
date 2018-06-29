package bitcoin.wallet.modules.transactions

import bitcoin.wallet.core.DatabaseChangeset
import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.core.managers.CoinManager
import bitcoin.wallet.entities.BlockchainInfo
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.entities.coins.bitcoinCash.BitcoinCash
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.util.*

class TransactionsInteractorTest {

    private val delegate = mock(TransactionsModule.IInteractorDelegate::class.java)
    private val databaseManager = mock(IDatabaseManager::class.java)
    private val coinManager = mock(CoinManager::class.java)

    private val interactor = TransactionsInteractor(databaseManager, coinManager)

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor.delegate = delegate
    }

    @Test
    fun retrieveTransactionRecords() {
        whenever(databaseManager.getTransactionRecords()).thenReturn(Observable.empty())

        interactor.retrieveTransactionRecords()

        verify(databaseManager).getTransactionRecords()
    }

    @Test
    fun retrieveTransactionItems_success() {
        val transactionRecords = listOf<TransactionRecord>()

        whenever(databaseManager.getTransactionRecords()).thenReturn(Observable.just(DatabaseChangeset(transactionRecords)))

        interactor.retrieveTransactionRecords()

        verify(delegate).didRetrieveTransactionRecords(listOf())
    }

    @Test
    fun retrieveTransactionItems_transactionOutConvert() {
        val now = Date()
        val bitcoin = Bitcoin()
        val bitcoinCash = BitcoinCash()
        val blockchainInfos = listOf(
                BlockchainInfo().apply {
                    coinCode = "BTC"
                    latestBlockHeight = 130
                },
                BlockchainInfo().apply {
                    coinCode = "BCH"
                    latestBlockHeight = 140
                })

        val transactionRecordBTC = TransactionRecord().apply {
            transactionHash = "transactionHash"
            amount = 100000000
            fee = 1000000
            incoming = true
            timestamp = now.time
            from = "from-address"
            to = "to-address"
            blockHeight = 0
            coinCode = "BTC"
        }

        val transactionRecordBCH = TransactionRecord().apply {
            transactionHash = "transactionHash"
            amount = 100000000
            fee = 1000000
            incoming = true
            timestamp = now.time
            from = "from-address"
            to = "to-address"
            blockHeight = 113
            coinCode = "BCH"
        }

        val expectedItems = listOf(
                TransactionRecordViewItem(
                        "transactionHash",
                        CoinValue(bitcoin, 1.0),
                        CoinValue(bitcoin, 0.01),
                        "from-address",
                        "to-address",
                        true,
                        0,
                        now,
                        TransactionRecordViewItem.Status.PENDING,
                        0
                ),
                TransactionRecordViewItem(
                        "transactionHash",
                        CoinValue(bitcoinCash, 1.0),
                        CoinValue(bitcoinCash, 0.01),
                        "from-address",
                        "to-address",
                        true,
                        113,
                        now,
                        TransactionRecordViewItem.Status.SUCCESS,
                        28
                )
        )

        whenever(coinManager.getCoinByCode("BTC")).thenReturn(bitcoin)
        whenever(coinManager.getCoinByCode("BCH")).thenReturn(bitcoinCash)
        whenever(databaseManager.getTransactionRecords()).thenReturn(Observable.just(DatabaseChangeset(listOf(transactionRecordBTC, transactionRecordBCH))))
        whenever(databaseManager.getBlockchainInfos()).thenReturn(Observable.just(DatabaseChangeset(blockchainInfos)))

        interactor.retrieveTransactionRecords()

        verify(delegate).didRetrieveTransactionRecords(expectedItems)
    }

}
