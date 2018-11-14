package io.horizontalsystems.bankwallet.modules.wallet

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.atLeast
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.BitcoinAdapter
import io.horizontalsystems.bankwallet.core.IExchangeRateManager
import io.horizontalsystems.bankwallet.core.managers.AdapterManager
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.coins.CoinOld
import io.horizontalsystems.bankwallet.entities.coins.bitcoin.Bitcoin
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@SuppressStaticInitializationFor("io.horizontalsystems.wallet.core.managers.ExchangeRateManager")
class WalletInteractorTest {

    private val delegate = mock(WalletModule.IInteractorDelegate::class.java)
    private val adapterManager = mock(AdapterManager::class.java)
    private val exchangeRateManager = mock(IExchangeRateManager::class.java)
    private val bitcoinAdapter = mock(BitcoinAdapter::class.java)
    private lateinit var interactor: WalletInteractor
    private var coin = Bitcoin()
    private var words = listOf("used", "ugly", "meat", "glad", "balance", "divorce", "inner", "artwork", "hire", "invest", "already", "piano")
    private var wordsHash = words.joinToString(" ")
    private var adapterId: String = "${wordsHash.hashCode()}-${coin.code}"
    private val currencyUsd = Currency(code = "USD", symbol = "\u0024")


    private var exchangeRates = mutableMapOf(Bitcoin() as CoinOld to CurrencyValue(currencyUsd, 10_000.0))

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor = WalletInteractor(adapterManager, exchangeRateManager)
        interactor.delegate = delegate

        adapterManager.adapters = mutableListOf(bitcoinAdapter)

        whenever(exchangeRateManager.getLatestExchangeRateSubject()).thenReturn(PublishSubject.create())
    }

    @Test
    fun fetchWalletBalances() {
        whenever(exchangeRateManager.getExchangeRates()).thenReturn(exchangeRates)
        whenever(adapterManager.subject).thenReturn(PublishSubject.create<Boolean>())

        interactor.notifyWalletBalances()

        verify(delegate).didInitialFetch(any(), any(), any())
    }

    @Test
    fun fetchWalletBalances_balanceUpdated() {
        val coin = Bitcoin()
        val newBalanceValue = 3.4
        val balanceSub: PublishSubject<Double> = PublishSubject.create()
        val managerSub: PublishSubject<Boolean> = PublishSubject.create()

        whenever(exchangeRateManager.getExchangeRates()).thenReturn(exchangeRates)

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
        val managerSub: PublishSubject<Boolean> = PublishSubject.create()

        whenever(adapterManager.subject).thenReturn(managerSub)
        whenever(exchangeRateManager.getExchangeRates()).thenReturn(exchangeRates)

        interactor.notifyWalletBalances()

        managerSub.onNext(true)

        verify(delegate, atLeast(2)).didInitialFetch(any(), any(), any())
    }

}
