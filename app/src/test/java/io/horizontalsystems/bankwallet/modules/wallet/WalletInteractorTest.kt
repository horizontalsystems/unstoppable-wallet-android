package io.horizontalsystems.bankwallet.modules.wallet

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class WalletInteractorTest {

    private val delegate = mock(WalletModule.IInteractorDelegate::class.java)

    private val walletManager = mock(IWalletManager::class.java)
    private val rateManager = mock(RateManager::class.java)
    private val currencyManager = mock(ICurrencyManager::class.java)
    private val wallet = mock(Wallet::class.java)
    private val adapter = mock(IAdapter::class.java)

    //
    // PublishSubjects
    //
    private val walletsSubject = PublishSubject.create<List<Wallet>>()
    private val balanceSubject = PublishSubject.create<Double>()
    private val stateSubject = PublishSubject.create<AdapterState>()
    private val rateSubject = PublishSubject.create<Boolean>()
    private val currencySubject = PublishSubject.create<Currency>()

    private lateinit var interactor: WalletInteractor

    @Before
    fun setup() {
        RxBaseTest.setup()

        val wallets = listOf(wallet)

        whenever(adapter.stateSubject).thenReturn(stateSubject)
        whenever(adapter.balanceSubject).thenReturn(balanceSubject)
        whenever(walletManager.walletsSubject).thenReturn(walletsSubject)
        whenever(rateManager.subject).thenReturn(rateSubject)
        whenever(currencyManager.subject).thenReturn(currencySubject)

        whenever(wallet.adapter).thenReturn(adapter)
        whenever(walletManager.wallets).thenReturn(wallets)

        interactor = WalletInteractor(walletManager, rateManager, currencyManager)
        interactor.delegate = delegate
    }

    @Test
    fun loadWallets() {
        interactor.loadWallets()

        adapter.stateSubject.onNext(AdapterState.Synced)
        verify(delegate).didUpdate()
    }
}
