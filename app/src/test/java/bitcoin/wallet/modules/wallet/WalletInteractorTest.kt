package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.DatabaseChangeset
import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.entities.*
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class WalletInteractorTest {

    private val delegate = mock(WalletModule.IInteractorDelegate::class.java)
    private val databaseManager = mock(IDatabaseManager::class.java)

    private var exchangeRates = DatabaseChangeset(listOf(
            ExchangeRate().apply {
                code = "BTC"
                value = 10_000.0
            }
    ))

    private var unspentOutputs = DatabaseChangeset(listOf(
            UnspentOutput().apply {
                value = 50_000_000
            },
            UnspentOutput().apply {
                value = 30_000_000
            }
    ))

    private lateinit var interactor: WalletInteractor

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor = WalletInteractor(databaseManager)
        interactor.delegate = delegate
    }

    @Test
    fun fetchWalletBalances() {
        whenever(databaseManager.getExchangeRates()).thenReturn(Observable.just(exchangeRates))
        whenever(databaseManager.getUnspentOutputs()).thenReturn(Observable.just(unspentOutputs))

        val expectedWalletBalances = listOf(
                WalletBalanceItem(CoinValue(Bitcoin(), 0.8), 10_000.0, DollarCurrency())
        )

        interactor.notifyWalletBalances()

        verify(delegate).didFetchWalletBalances(expectedWalletBalances)
    }

    @Test
    fun fetchWalletBalances_emptyRates() {
        whenever(databaseManager.getExchangeRates()).thenReturn(Observable.just(DatabaseChangeset(listOf())))
        whenever(databaseManager.getUnspentOutputs()).thenReturn(Observable.just(unspentOutputs))

        interactor.notifyWalletBalances()

        verify(delegate, never()).didFetchWalletBalances(any())
    }

}
