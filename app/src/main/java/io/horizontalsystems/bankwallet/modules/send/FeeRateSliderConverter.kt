package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.FeeRates

class FeeRateSliderConverter(feeRates: FeeRates) {

    private val highestNew: Int
    private val low: Int = feeRates.lowest
    private val medium: Int = feeRates.medium
    private val high: Int = feeRates.highest

    init {
        val totalRange = high - low
        val lowMediumRange = medium - low

        if (totalRange < 0 || lowMediumRange < 0)
            throw Exception()

        val pos = lowMediumRange/totalRange * 100
        val correctedPos = Math.min(Math.max(20, pos), 80)
        val highestNewFloat = lowMediumRange.toFloat() * 100 / correctedPos.toFloat()

        highestNew = highestNewFloat.toInt()

        if (highestNew < 0)
            throw Exception()
    }

    fun percent(unit: Int): Int {
        return (unit - low) * 100 / highestNew
    }

    fun unit(percent: Int): Int {
        return highestNew * percent / 100 + low
    }

}
