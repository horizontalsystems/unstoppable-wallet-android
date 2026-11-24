package io.horizontalsystems.walletkit.modules.evmfee

import io.horizontalsystems.ethereumkit.core.eip1559.FeeHistory
import io.horizontalsystems.walletkit.modules.evmfee.eip1559.Eip1559GasPriceService
import io.horizontalsystems.walletkit.modules.evmfee.legacy.LegacyGasPriceService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.FeePriority
import org.junit.Assert.assertEquals
import org.junit.Test

class PriorityFeeTest {

    private fun tip(ratio: List<Double>, reward: List<List<Long>>, feePriority: FeePriority): Long =
        Eip1559GasPriceService.recommendedPriorityFee(
            FeeHistory(baseFeePerGas = listOf(100), gasUsedRatio = ratio, oldestBlock = 1, reward = reward),
            feePriority
        )

    @Test
    fun rewardPercentiles_priorityAddsTheHigherPercentile() {
        assertEquals(listOf(50), Eip1559GasPriceService.rewardPercentiles(FeePriority.Standard))
        assertEquals(listOf(50, 75), Eip1559GasPriceService.rewardPercentiles(FeePriority.Priority))
    }

    @Test
    fun standardTip_isMeanOfFirstColumnOverAllBlocks() {
        val tip = tip(listOf(0.5, 0.0, 0.3), listOf(listOf(10L), listOf(0L), listOf(20L)), FeePriority.Standard)

        assertEquals(10, tip)
    }

    @Test
    fun standardTip_ignoresTheHigherPercentileColumn() {
        val tip = tip(listOf(0.5, 0.5), listOf(listOf(10L, 300L), listOf(30L, 500L)), FeePriority.Standard)

        assertEquals(20, tip)
    }

    @Test
    fun priorityTip_isMedianForOddCount() {
        val reward = listOf(listOf(10L, 30L), listOf(10L, 50L), listOf(10L, 40L))

        assertEquals(40, tip(listOf(0.5, 0.5, 0.5), reward, FeePriority.Priority))
    }

    @Test
    fun priorityTip_isMeanOfTheMiddlePairForEvenCount() {
        val reward = listOf(listOf(10L, 30L), listOf(10L, 50L), listOf(10L, 40L), listOf(10L, 60L))

        assertEquals(45, tip(listOf(0.5, 0.5, 0.5, 0.5), reward, FeePriority.Priority))
    }

    @Test
    fun priorityTip_medianOfNearMaxRewardsDoesNotOverflow() {
        val reward = listOf(listOf(1L, Long.MAX_VALUE), listOf(1L, Long.MAX_VALUE - 2))

        assertEquals(Long.MAX_VALUE - 1, tip(listOf(0.5, 0.5), reward, FeePriority.Priority))
    }

    @Test
    fun emptyBlock_isLeftOutOfTheMedianButCountedInTheMean() {
        val ratio = listOf(0.5, 0.0, 0.5, 0.5)
        val reward = listOf(listOf(20L, 30L), listOf(0L, 0L), listOf(20L, 40L), listOf(20L, 50L))

        // Median of 30, 40, 50; the empty block would make it 35
        assertEquals(40, tip(ratio, reward, FeePriority.Priority))
        // Mean of 20, 0, 20, 20
        assertEquals(15, tip(ratio, reward, FeePriority.Standard))
    }

    @Test
    fun priorityTip_isNotMovedByAnOutlier() {
        val ratio = listOf(0.5, 0.5, 0.5, 0.5, 0.5)
        val reward = listOf(listOf(10L, 30L), listOf(10L, 40L), listOf(10L, 50L), listOf(10L, 45L), listOf(10L, 10000L))

        assertEquals(45, tip(ratio, reward, FeePriority.Priority))
    }

    @Test
    fun priorityTip_isNeverBelowTheStandardTip() {
        // Standard mean = (10 + 10 + 100) / 3 = 40; priority median = 20
        val reward = listOf(listOf(10L, 20L), listOf(10L, 20L), listOf(100L, 200L))

        assertEquals(40, tip(listOf(0.5, 0.5, 0.5), reward, FeePriority.Priority))
    }

    @Test
    fun priorityTip_fallsBackToStandardWhenAllBlocksAreEmpty() {
        val reward = listOf(listOf(10L, 30L), listOf(20L, 50L), listOf(30L, 40L))

        assertEquals(20, tip(listOf(0.0, 0.0, 0.0), reward, FeePriority.Priority))
    }

    @Test
    fun priorityTip_fallsBackToStandardWhenTheHigherPercentileIsMissing() {
        val reward = listOf(listOf(10L), listOf(30L))

        assertEquals(20, tip(listOf(0.5, 0.5), reward, FeePriority.Priority))
    }

    @Test
    fun noRewards_giveZeroTip() {
        assertEquals(0, tip(emptyList(), emptyList(), FeePriority.Standard))
        assertEquals(0, tip(emptyList(), emptyList(), FeePriority.Priority))
    }

    @Test
    fun legacyGasPrice_isRaisedOnlyForPriority() {
        assertEquals(100, LegacyGasPriceService.prioritized(100, FeePriority.Standard))
        assertEquals(120, LegacyGasPriceService.prioritized(100, FeePriority.Priority))
    }

    @Test
    fun legacyGasPrice_isRoundedUp() {
        assertEquals(2, LegacyGasPriceService.prioritized(1, FeePriority.Priority))
    }

    @Test
    fun legacyGasPrice_thatWouldOverflowIsKept() {
        val price = Long.MAX_VALUE / 100

        assertEquals(price, LegacyGasPriceService.prioritized(price, FeePriority.Priority))
    }
}
