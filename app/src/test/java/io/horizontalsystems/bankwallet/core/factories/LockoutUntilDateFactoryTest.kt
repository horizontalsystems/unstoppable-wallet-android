package io.horizontalsystems.bankwallet.core.factories

import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.core.ICurrentDateProvider
import io.horizontalsystems.pin.core.LockoutUntilDateFactory
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import java.util.*

class LockoutUntilDateFactoryTest {

    private val currentDateProvider = Mockito.mock(ICurrentDateProvider::class.java)
    private val factory = LockoutUntilDateFactory(currentDateProvider)
    var lockoutTimeStamp = 1L
    var uptime = 1L


    @Test
    fun testUnlockTime_0Min() {
        val currentDate = Date()
        whenever(currentDateProvider.currentDate).thenReturn(currentDate)
        uptime = 4000L
        Assert.assertEquals(factory.lockoutUntilDate(5, lockoutTimeStamp, uptime), currentDate)
    }

    @Test
    fun testUnlockTime_5Min() {
        val date = Date()
        val date2 = Date()
        whenever(currentDateProvider.currentDate).thenReturn(date)
        date2.time = date.time + 5 * 60 * 1000
        Assert.assertEquals(factory.lockoutUntilDate(5, lockoutTimeStamp, uptime), date2)
    }

    @Test
    fun testUnlockTime_10Min() {
        val date = Date()
        val date2 = Date()
        whenever(currentDateProvider.currentDate).thenReturn(date)
        date2.time = date.time + 10 * 60 * 1000
        Assert.assertEquals(factory.lockoutUntilDate(6, lockoutTimeStamp, uptime), date2)
    }

    @Test
    fun testUnlockTime_15Min() {
        val date = Date()
        val date2 = Date()
        whenever(currentDateProvider.currentDate).thenReturn(date)
        date2.time = date.time + 15 * 60 * 1000
        Assert.assertEquals(factory.lockoutUntilDate(7, lockoutTimeStamp, uptime), date2)
    }

    @Test
    fun testUnlockTime_30Min() {
        val date = Date()
        val date2 = Date()
        whenever(currentDateProvider.currentDate).thenReturn(date)
        date2.time = date.time + 30 * 60 * 1000
        Assert.assertEquals(factory.lockoutUntilDate(8, lockoutTimeStamp, uptime), date2)
    }

    @Test
    fun testUnlockTime_MoreThan30Min() {
        val date = Date()
        val date2 = Date()
        whenever(currentDateProvider.currentDate).thenReturn(date)
        date2.time = date.time + 30 * 60 * 1000
        Assert.assertEquals(factory.lockoutUntilDate(9, lockoutTimeStamp, uptime), date2)
    }

}
