package io.horizontalsystems.bankwallet.viewHelpers

import org.junit.Test

import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.*

class DateHelperTest {

    @Test
    fun testRoundDate() {

        val dateStr1 = "31/12/2020 23:01"
        val dateStr2 = "01/01/2021 00:00"
        val dateStr3 = "31/12/2020 23:00"

        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm")

        var dateRoundHour = formatter.parse(dateStr1)
        val dateRoundedCeiling = formatter.parse(dateStr2)
        val dateRounded = formatter.parse(dateStr3)

        assertEquals(DateHelper.roundDate(dateRoundHour,Calendar.HOUR, true), dateRoundedCeiling)
        assertEquals(DateHelper.roundDate(dateRoundHour,Calendar.HOUR, false), dateRounded)
    }
}