package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.IExchangeRateProvider
import bitcoin.wallet.core.IUnspentOutputProvider
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
    private val unspentOutputProvider = mock(IUnspentOutputProvider::class.java)
    private val exchangeRateProvider = mock(IExchangeRateProvider::class.java)

    private lateinit var interactor: WalletInteractor

    private lateinit var unspentOutputSubject: PublishSubject<List<UnspentOutput>>

    private lateinit var exchangeRateSubject: PublishSubject<HashMap<Coin, Double>>

    private var exchangeRate = 10_000.0

    @Before
    fun before() {
        RxBaseTest.setup()

        unspentOutputSubject = PublishSubject.create<List<UnspentOutput>>()
        exchangeRateSubject = PublishSubject.create<HashMap<Coin, Double>>()

        whenever(unspentOutputProvider.subject).thenReturn(unspentOutputSubject)
        whenever(exchangeRateProvider.subject).thenReturn(exchangeRateSubject)

        val unspentOutput1 = mock(UnspentOutput::class.java)
        val unspentOutput2 = mock(UnspentOutput::class.java)

        whenever(unspentOutput1.value).thenReturn((0.5 * 100000000).toLong())
        whenever(unspentOutput2.value).thenReturn((0.3 * 100000000).toLong())

        whenever(exchangeRateProvider.getExchangeRateForCoin(Bitcoin())).thenReturn(exchangeRate)
        whenever(unspentOutputProvider.unspentOutputs).thenReturn(listOf(unspentOutput1, unspentOutput2))

        interactor = WalletInteractor(unspentOutputProvider, exchangeRateProvider)
        interactor.delegate = delegate

    }

    @Test
    fun fetchWalletBalances() {
        val expectedWalletBalances = listOf(
                WalletBalanceItem(CoinValue(Bitcoin(), 0.8), exchangeRate, DollarCurrency())
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
                WalletBalanceItem(CoinValue(Bitcoin(), 3.0), exchangeRate, DollarCurrency())
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

        exchangeRateSubject.onNext(hashMapOf(Bitcoin() to exchangeRate))

        verify(delegate).didFetchWalletBalances(walletBalances)
    }

}