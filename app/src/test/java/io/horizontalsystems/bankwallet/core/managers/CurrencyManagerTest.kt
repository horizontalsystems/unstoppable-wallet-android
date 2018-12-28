package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.entities.Currency
import org.junit.Test
import org.mockito.Mockito.mock

class CurrencyManagerTest {

    private lateinit var currencyManager: CurrencyManager
    private var localStorage = mock(ILocalStorage::class.java)
    private var appConfigProvider = mock(AppConfigProvider::class.java)

    @Test
    fun initialBaseCurrency_emptyList() {
        whenever(appConfigProvider.currencies).thenReturn(listOf())

        currencyManager = CurrencyManager(localStorage, appConfigProvider)

        currencyManager.baseCurrencyObservable
                .test()
                .assertEmpty()

    }

    @Test
    fun initialBaseCurrency_firstFromList() {
        val currency1 = mock(Currency::class.java)
        val currency2 = mock(Currency::class.java)

        whenever(appConfigProvider.currencies).thenReturn(listOf(currency1, currency2))

        currencyManager = CurrencyManager(localStorage, appConfigProvider)

        currencyManager.baseCurrencyObservable
                .test()
                .awaitCount(1)
                .assertValue(currency1)

    }

    @Test
    fun initialBaseCurrency_stored() {
        val currency1 = mock(Currency::class.java)
        val currency2 = mock(Currency::class.java)
        val code2 = "code2"

        whenever(appConfigProvider.currencies).thenReturn(listOf(currency1, currency2))
        whenever(localStorage.baseCurrencyCode).thenReturn(code2)
        whenever(currency2.code).thenReturn(code2)

        currencyManager = CurrencyManager(localStorage, appConfigProvider)

        currencyManager.baseCurrencyObservable
                .test()
                .assertValue(currency2)
    }

    @Test
    fun initialBaseCurrency_setBaseCurrency() {
        val currencyCode = "currencyCode"
        val currency1 = mock(Currency::class.java)
        val currency2 = mock(Currency::class.java)

        whenever(appConfigProvider.currencies)
                .thenReturn(listOf())
                .thenReturn(listOf(currency1, currency2))

        whenever(currency2.code).thenReturn(currencyCode)

        currencyManager = CurrencyManager(localStorage, appConfigProvider)
        currencyManager.setBaseCurrency2(currencyCode)

        verify(localStorage).baseCurrencyCode = currencyCode

        currencyManager.baseCurrencyObservable
                .test()
                .assertValue(currency2)
    }
}