package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test

class RateSyncSchedulerTest {

    private lateinit var rateSyncScheduler: RateSyncScheduler

    private val rateManager = mock<RateManager>()
    private val currencyManager = mock<ICurrencyManager>()
    private val connectivityManager = mock<ConnectivityManager>()
    private val walletManager = mock<IWalletManager>()

    private val walletsUpdatedSignal = PublishSubject.create<Unit>()
    private val baseCurrencyUpdatedSignal = PublishSubject.create<Unit>()
    private val timerSignal = BehaviorSubject.createDefault(Unit)
    private val networkAvailabilitySignal = PublishSubject.create<Unit>()

    @Before
    fun setup() {
        RxBaseTest.setup()

        whenever(walletManager.walletsUpdatedSignal).thenReturn(walletsUpdatedSignal)
        whenever(currencyManager.baseCurrencyUpdatedSignal).thenReturn(baseCurrencyUpdatedSignal)
        whenever(connectivityManager.networkAvailabilitySignal).thenReturn(networkAvailabilitySignal)
    }

    @Test
    fun init() {
        rateSyncScheduler = RateSyncScheduler(rateManager, walletManager, currencyManager, connectivityManager, timerSignal)

        verify(rateManager).syncLatestRates()
        verifyNoMoreInteractions(rateManager)
    }

    @Test
    fun onTimePassed() {
        rateSyncScheduler = RateSyncScheduler(rateManager, walletManager, currencyManager, connectivityManager, timerSignal)

        timerSignal.onNext(Unit)

        verify(rateManager, times(2)).syncLatestRates()
    }

    @Test
    fun onBaseCurrencyUpdate() {
        rateSyncScheduler = RateSyncScheduler(rateManager, walletManager, currencyManager, connectivityManager, timerSignal)

        baseCurrencyUpdatedSignal.onNext(Unit)

        verify(rateManager, times(2)).syncLatestRates()
    }

}
