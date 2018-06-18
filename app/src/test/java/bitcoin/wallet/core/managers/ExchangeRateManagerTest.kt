package bitcoin.wallet.core.managers

import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.core.INetworkManager
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class ExchangeRateManagerTest {

    private val databaseManager = Mockito.mock(IDatabaseManager::class.java)
    private val networkManager = Mockito.mock(INetworkManager::class.java)
    private val updateSubject = mock<PublishSubject<HashMap<String, Double>>>()

    private val exchangeRateManager = ExchangeRateManager(databaseManager, networkManager, updateSubject)

    private val exchangeRates = hashMapOf("BTC" to 10.0, "ETH" to 20.0)

    @Before
    fun before() {
        RxBaseTest.setup()

        whenever(networkManager.getExchangeRates()).thenReturn(Flowable.just(exchangeRates))
    }

    @Test
    fun refresh() {
        exchangeRateManager.refresh()

        verify(databaseManager).truncateExchangeRates()
        verify(databaseManager).insertExchangeRates(exchangeRates)

    }

    @Test
    fun refresh_subjectCall() {
        exchangeRateManager.refresh()

        verify(updateSubject).onNext(exchangeRates)
    }

}
