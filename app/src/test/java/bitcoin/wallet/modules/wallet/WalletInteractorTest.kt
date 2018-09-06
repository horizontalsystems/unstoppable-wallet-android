package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.BitcoinAdapter
import bitcoin.wallet.core.DatabaseChangeset
import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.ExchangeRate
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.atLeast
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class WalletInteractorTest {

    private val delegate = mock(WalletModule.IInteractorDelegate::class.java)
    private val databaseManager = mock(IDatabaseManager::class.java)
    private val adapterManager = mock(AdapterManager::class.java)
    private val bitcoinAdapter = mock(BitcoinAdapter::class.java)
    private lateinit var interactor: WalletInteractor
    private var coin = Bitcoin()
    private var words = listOf("used", "ugly", "meat", "glad", "balance", "divorce", "inner", "artwork", "hire", "invest", "already", "piano")
    private var wordsHash = words.joinToString(" ")
    private var adapterId: String = "${wordsHash.hashCode()}-${coin.code}"

    private var exchangeRates = DatabaseChangeset(listOf(
            ExchangeRate().apply {
                code = "BTC"
                value = 10_000.0
            }
    ))

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor = WalletInteractor(adapterManager, databaseManager)
        interactor.delegate = delegate

        adapterManager.adapters = mutableListOf(bitcoinAdapter)
    }

    @Test
    fun fetchWalletBalances() {
        whenever(databaseManager.getExchangeRates()).thenReturn(Observable.just(exchangeRates))
        whenever(adapterManager.subject).thenReturn(PublishSubject.create<Any>())

        interactor.notifyWalletBalances()

        verify(delegate).didInitialFetch(any(), any(), any(), any())
    }

    @Test
    fun fetchWalletBalances_balanceUpdated() {
        val coin = Bitcoin()
        val newBalanceValue = 3.4
        val balanceSub: PublishSubject<Double> = PublishSubject.create()
        val managerSub: PublishSubject<Any> = PublishSubject.create()

        whenever(databaseManager.getExchangeRates()).thenReturn(Observable.just(DatabaseChangeset(listOf())))

        whenever(adapterManager.subject).thenReturn(managerSub)
        whenever(adapterManager.adapters).thenReturn(mutableListOf(bitcoinAdapter))

        whenever(bitcoinAdapter.id).thenReturn(adapterId)
        whenever(bitcoinAdapter.coin).thenReturn(coin)
        whenever(bitcoinAdapter.balanceSubject).thenReturn(balanceSub)
        whenever(bitcoinAdapter.balance).thenReturn(0.0)

        interactor.notifyWalletBalances()
        balanceSub.onNext(newBalanceValue)
        val expectedCoinValue = CoinValue(Bitcoin(), newBalanceValue)

        verify(delegate).didUpdate(expectedCoinValue, adapterId)
    }

    @Test
    fun notifyWalletBalances_adapterManagerSubjectUpdate() {
        val managerSub: PublishSubject<Any> = PublishSubject.create()

        whenever(adapterManager.subject).thenReturn(managerSub)
        whenever(databaseManager.getExchangeRates()).thenReturn(Observable.just(exchangeRates))

        interactor.notifyWalletBalances()

        managerSub.onNext(Any())

        verify(delegate, atLeast(2)).didInitialFetch(any(), any(), any(), any())
    }

}
