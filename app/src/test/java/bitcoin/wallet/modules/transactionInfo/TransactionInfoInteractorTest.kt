package bitcoin.wallet.modules.transactionInfo

import bitcoin.wallet.core.DatabaseChangeset
import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.core.managers.CoinManager
import bitcoin.wallet.entities.BlockchainInfo
import bitcoin.wallet.entities.ExchangeRate
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.modules.RxBaseTest
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class TransactionInfoInteractorTest {

    private val delegate = Mockito.mock(TransactionInfoModule.IInteractorDelegate::class.java)
    private val databaseManager = Mockito.mock(IDatabaseManager::class.java)
    private val coinManager = Mockito.mock(CoinManager::class.java)

    private val interactor = TransactionInfoInteractor(databaseManager, coinManager)

    @Before
    fun setUp() {
        RxBaseTest.setup()

        interactor.delegate = delegate
    }

    @Test
    fun getTransactionInfo() {
        val coinCode = "BTC"
        val bitcoin = Bitcoin()

        val txHash = "tx_hash"
        val txRecord = TransactionRecord().apply { this.coinCode = coinCode }
        val txRecordViewItem = Mockito.mock(TransactionRecordViewItem::class.java)

        val btcLatestBlockHeight = 105L
        val exchangeRate = ExchangeRate().apply {
            code = coinCode
            value = 7349.4
        }

        val blockchainInfos = listOf(
                BlockchainInfo().apply {
                    this.coinCode = "BTC"
                    this.latestBlockHeight = btcLatestBlockHeight
                })

        whenever(databaseManager.getBlockchainInfos()).thenReturn(Observable.just(DatabaseChangeset(blockchainInfos)))
        whenever(databaseManager.getTransactionRecord(any(), any())).thenReturn(Observable.just(txRecord))
        whenever(coinManager.getCoinByCode(coinCode)).thenReturn(bitcoin)
        whenever(databaseManager.getExchangeRates()).thenReturn(Observable.just(DatabaseChangeset(arrayListOf(exchangeRate))))

//        whenever(transactionConverter.convertToTransactionRecordViewItem(bitcoin, txRecord, btcLatestBlockHeight, exchangeRate.value)).thenReturn(txRecordViewItem)

        interactor.getTransactionInfo(coinCode, txHash)

        verify(delegate).didGetTransactionInfo(txRecordViewItem)
    }

}
