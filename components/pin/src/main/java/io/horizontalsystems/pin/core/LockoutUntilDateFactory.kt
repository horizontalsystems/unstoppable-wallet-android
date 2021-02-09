package io.horizontalsystems.pin.core

import io.horizontalsystems.core.ICurrentDateProvider
import java.util.*

class LockoutUntilDateFactory(private val currentDateProvider: ICurrentDateProvider) : ILockoutUntilDateFactory {

    override fun lockoutUntilDate(failedAttempts: Int, lockoutUptime: Long, uptime: Long): Date? {

        var timeFrame: Long = 0

        val timeDiff = if (uptime >= lockoutUptime) uptime - lockoutUptime else uptime

        when {
            failedAttempts == 5 -> timeFrame = 5 * 60 * 1000 - timeDiff
            failedAttempts == 6 -> timeFrame = 10 * 60 * 1000 - timeDiff
            failedAttempts == 7 -> timeFrame = 15 * 60 * 1000 - timeDiff
            failedAttempts >= 8 -> timeFrame = 30 * 60 * 1000 - timeDiff
        }

        return when {
            timeFrame > 0 -> {
                val timestamp = timeFrame + currentDateProvider.currentDate.time
                val date = currentDateProvider.currentDate
                date.time = timestamp
                date
            }
            else -> null
        }
    }

}
