package io.horizontalsystems.walletkit.core.providers

import org.junit.Assert.assertEquals
import org.junit.Test

class BitcoinFeeRateProviderTest {

    @Test
    fun recommendedRate_isTheFastestRate() {
        assertEquals(20, BitcoinFeeRateProvider.recommendedRate(fastest = 20, halfHour = 10))
    }

    @Test
    fun recommendedRate_isNeverBelowTheHalfHourRate() {
        assertEquals(10, BitcoinFeeRateProvider.recommendedRate(fastest = 8, halfHour = 10))
        assertEquals(10, BitcoinFeeRateProvider.recommendedRate(fastest = 0, halfHour = 10))
    }
}
