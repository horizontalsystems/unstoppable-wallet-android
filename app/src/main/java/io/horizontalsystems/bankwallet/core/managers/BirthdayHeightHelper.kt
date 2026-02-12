package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.monerokit.MoneroKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Calendar
import java.util.Date

object BirthdayHeightHelper {

    val MAX_BIRTHDAY_HEIGHT: Long = UInt.MAX_VALUE.toLong()

    fun minBirthdayHeight(blockchainType: BlockchainType): Long = when (blockchainType) {
        BlockchainType.Zcash -> 420_000L
        BlockchainType.Monero -> 0L
        else -> 0L
    }

    fun getFirstBlockDate(blockchainType: BlockchainType) = when (blockchainType) {
        BlockchainType.Zcash -> LocalDate.of(2018, 10, 29)
        BlockchainType.Monero -> LocalDate.of(2014, 4, 18)
        else -> throw IllegalArgumentException()
    }

    fun getInitialDateForPicker(cachedDate: Date?): LocalDate {
        if (cachedDate == null) return LocalDate.now()
        val calendar = Calendar.getInstance().apply { time = cachedDate }
        return LocalDate.of(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
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
