package cash.p.terminal.modules.multiswap

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PriceImpactServiceTest {

    private val service = PriceImpactService()

    @Test
    fun setFiatAmounts_smallNegativeDifference_returnsNegativePercent() {
        service.setFiatAmountIn(BigDecimal("49.99"))
        service.setFiatAmountOut(BigDecimal("49.89"))

        val state = service.stateFlow.value

        assertEquals(BigDecimal("-0.2"), state.fiatPriceImpact)
        assertEquals(PriceImpactLevel.Warning, state.fiatPriceImpactLevel)
    }

    @Test
    fun setFiatAmounts_smallPositiveDifference_returnsPositivePercent() {
        service.setFiatAmountIn(BigDecimal("49.89"))
        service.setFiatAmountOut(BigDecimal("49.99"))

        val state = service.stateFlow.value

        assertEquals(BigDecimal("0.2"), state.fiatPriceImpact)
        assertEquals(PriceImpactLevel.Good, state.fiatPriceImpactLevel)
    }
}
