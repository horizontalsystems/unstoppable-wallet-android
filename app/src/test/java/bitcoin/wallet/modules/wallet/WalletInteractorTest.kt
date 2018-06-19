package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.entities.*
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class WalletInteractorTest {

    private val delegate = mock(WalletModule.IInteractorDelegate::class.java)
    private val databaseManager = mock(IDatabaseManager::class.java)

    private lateinit var interactor: WalletInteractor

    private val unspentOutputSubject = PublishSubject.create<List<UnspentOutput>>()
    private val exchangeRateSubject = PublishSubject.create<List<ExchangeRate>>()

    private val exchangeRates = listOf(ExchangeRate("BTC", 10_000.0))

    @Before
    fun before() {
        RxBaseTest.setup()

        val unspentOutput1 = mock(UnspentOutput::class.java)
        val unspentOutput2 = mock(UnspentOutput::class.java)

        whenever(unspentOutput1.value).thenReturn((0.5 * 100000000).toLong())
        whenever(unspentOutput2.value).thenReturn((0.3 * 100000000).toLong())

        whenever(databaseManager.getExchangeRates()).thenReturn(exchangeRates)
        whenever(databaseManager.getUnspentOutputs()).thenReturn(listOf(unspentOutput1, unspentOutput2))

        interactor = WalletInteractor(databaseManager, unspentOutputSubject, exchangeRateSubject)
        interactor.delegate = delegate

    }

    @Test
    fun fetchWalletBalances() {
        val expectedWalletBalances = listOf(
                WalletBalanceItem(CoinValue(Bitcoin(), 0.8), 10_000.0, DollarCurrency())
        )

        interactor.notifyWalletBalances()

        verify(delegate).didFetchWalletBalances(expectedWalletBalances)

    }

    @Test
    fun updateBalancesData() {
        val unspentOutput1 = mock(UnspentOutput::class.java)
        val unspentOutput2 = mock(UnspentOutput::class.java)

        whenever(unspentOutput1.value).thenReturn((1 * 100000000).toLong())
        whenever(unspentOutput2.value).thenReturn((2 * 100000000).toLong())

        val updatedUnspentOutputs = listOf(unspentOutput1, unspentOutput2)

        val walletBalances = listOf(
                WalletBalanceItem(CoinValue(Bitcoin(), 3.0), 10_000.0, DollarCurrency())
        )

        unspentOutputSubject.onNext(updatedUnspentOutputs)

        verify(delegate).didFetchWalletBalances(walletBalances)

    }

    @Test
    fun updateExchangeRatesData() {
        val exchangeRate = 20_000.0

        val walletBalances = listOf(
                WalletBalanceItem(CoinValue(Bitcoin(), 0.8), exchangeRate, DollarCurrency())
        )

        exchangeRateSubject.onNext(listOf(ExchangeRate(Bitcoin().code, exchangeRate)))

        verify(delegate).didFetchWalletBalances(walletBalances)
    }

}
