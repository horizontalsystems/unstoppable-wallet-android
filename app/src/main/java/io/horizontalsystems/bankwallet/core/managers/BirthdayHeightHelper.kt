package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.monerokit.MoneroKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

object BirthdayHeightHelper {

    val MAX_BIRTHDAY_HEIGHT: Long = UInt.MAX_VALUE.toLong()

    fun minBirthdayHeight(blockchainType: BlockchainType): Long = when (blockchainType) {
        BlockchainType.Zcash -> 420_000L
        BlockchainType.Monero -> 0L
        else -> 0L
    }

    fun datePickerYears(blockchainType: BlockchainType): List<Int> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val startYear = when (blockchainType) {
            BlockchainType.Zcash -> 2018
            BlockchainType.Monero -> 2014
            else -> 2009
        }
        return (startYear..currentYear).toList()
    }

    fun getInitialDateForPicker(cachedDate: Date?): Triple<Int, Int, Int> {
        val date = cachedDate ?: Date()
        val calendar = Calendar.getInstance().apply { time = date }
        return Triple(
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.YEAR)
        )
    }

    fun formatBlockDate(date: Date): String {
        return DateHelper.formatDate(date, "MMM d, yyyy")
    }

    suspend fun estimateBlockDate(blockchainType: BlockchainType, height: Long): Date? = withContext(Dispatchers.IO) {
        when (blockchainType) {
            BlockchainType.Zcash -> {
                ZcashAdapter.estimateBirthdayDate(App.instance, height)
            }
            BlockchainType.Monero -> {
                MoneroKit.dateForRestoreHeight(height)
            }
            else -> null
        }
    }

    suspend fun estimateBlockHeightFromDate(blockchainType: BlockchainType, day: Int, month: Int, year: Int): Long? = withContext(Dispatchers.Default) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val selectedDate = Date(calendar.timeInMillis)

        when (blockchainType) {
            BlockchainType.Zcash -> {
                ZcashAdapter.estimateBirthdayHeight(App.instance, selectedDate)
            }
            BlockchainType.Monero -> {
                MoneroKit.restoreHeightForDate(selectedDate)
            }
            else -> null
        }
    }

    fun isHeightValid(blockchainType: BlockchainType, height: Long?): Boolean {
        if (height == null) return false
        val min = minBirthdayHeight(blockchainType)
        return height in min..MAX_BIRTHDAY_HEIGHT
    }

    fun isDateInFuture(date: Date?): Boolean {
        return date != null && date.after(Date())
    }
}
