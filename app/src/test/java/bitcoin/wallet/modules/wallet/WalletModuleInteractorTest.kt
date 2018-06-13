package bitcoin.wallet.modules.wallet

import bitcoin.wallet.entities.Coin
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class WalletModuleInteractorTest {

    private val interactor = WalletModuleInteractor()
    private val delegate = mock(WalletModule.IInteractorDelegate::class.java)
    private val balanceDataProvider = mock(WalletModule.ICoinsDataProvider::class.java)

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor.coinsDataProvider = balanceDataProvider
        interactor.delegate = delegate
    }

    @Test
    fun retrieveInitialData() {
        interactor.retrieveInitialData()

        verify(balanceDataProvider).getCoins()
    }

    @Test
    fun retrieveInitialData_success_didBalanceItemsRetrieved() {
        val balance = Coin("Bitcoin", "BTC", 1.0, 7000.0, 7000.0)
        val balances = listOf(balance)

        whenever(balanceDataProvider.getCoins()).thenReturn(Flowable.just(balances))

        interactor.retrieveInitialData()

        verify(delegate).didCoinItemsRetrieved(balances)

    }

    @Test
    fun retrieveInitialData_success_didTotalFiatBalanceRetrieved() {
        val balance1 = mock(Coin::class.java)
        val balance2 = mock(Coin::class.java)
        val balances = listOf(balance1, balance2)

        whenever(balance1.amountFiat).thenReturn(1000.25)
        whenever(balance2.amountFiat).thenReturn(10000.50)

        whenever(balanceDataProvider.getCoins()).thenReturn(Flowable.just(balances))

        interactor.retrieveInitialData()

        verify(delegate).didTotalBalanceRetrieved(11000.75)

    }
}