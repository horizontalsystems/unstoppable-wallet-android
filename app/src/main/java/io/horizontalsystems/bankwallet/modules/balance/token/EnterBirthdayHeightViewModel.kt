package io.horizontalsystems.bankwallet.modules.balance.token

import android.util.Log
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.monerokit.MoneroKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class EnterBirthdayHeightViewModel(
    private val blockchainType: BlockchainType,
    private val account: Account,
    private val currentBirthdayHeight: Long?,
    private val restoreSettingsManager: RestoreSettingsManager
) : ViewModelUiState<EnterBirthdayHeightModule.UiState>() {

    private var birthdayHeight: Long? = null
    private var birthdayHeightText: String? = null
    private var blockDateText: String? = getBlockDateText(currentBirthdayHeight)
    private var rescanButtonEnabled: Boolean = false
    private var closeAfterRescan: Boolean = false
    private var datePickerLoading: Boolean = false
    private var closeDatePicker: Boolean = false

    override fun createState() = EnterBirthdayHeightModule.UiState(
        birthdayHeight = birthdayHeight,
        birthdayHeightText = birthdayHeightText,
        blockDateText = blockDateText,
        rescanButtonEnabled = rescanButtonEnabled,
        closeAfterRescan = closeAfterRescan,
        datePickerLoading = datePickerLoading,
        closeDatePicker = closeDatePicker
    )

    fun setBirthdayHeight(heightText: String) {
        val height = heightText.toLongOrNull()
        val isValid = height != null && height > 0
        val isDifferent = height != currentBirthdayHeight

        // When input is cleared, show the current birthday height's date
        val dateHeight = height ?: currentBirthdayHeight

        birthdayHeight = height
        birthdayHeightText = null // Reset so LaunchedEffect can trigger again for same value
        blockDateText = getBlockDateText(dateHeight)
        rescanButtonEnabled = isValid && isDifferent
        emitState()
    }

    fun onRescanClick() {
        val newHeight = birthdayHeight ?: return

        val settings = RestoreSettings()
        settings.birthdayHeight = newHeight
        restoreSettingsManager.save(settings, account, blockchainType)

        closeAfterRescan = true
        emitState()
    }

    fun onRescanHandled() {
        closeAfterRescan = false
        emitState()
    }

    fun onDatePickerOpened() {
        datePickerLoading = false
        closeDatePicker = false
        emitState()
    }

    fun onDatePickerClosed() {
        closeDatePicker = false
        emitState()
    }

    fun getInitialDateForPicker(): Triple<Int, Int, Int> {
        val height = birthdayHeight ?: currentBirthdayHeight
        val date = height?.let { estimateBlockDate(it) } ?: Date()
        val calendar = java.util.Calendar.getInstance().apply { time = date }
        return Triple(
            calendar.get(java.util.Calendar.DAY_OF_MONTH),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.YEAR)
        )
    }

    fun onDateSelected(day: Int, month: Int, year: Int) {
        datePickerLoading = true
        emitState()

        viewModelScope.launch {
            val estimatedHeight = estimateBlockHeightFromDate(day, month, year)
            if (estimatedHeight != null) {
                val isValid = estimatedHeight > 0
                val isDifferent = estimatedHeight != currentBirthdayHeight

                birthdayHeight = estimatedHeight
                birthdayHeightText = estimatedHeight.toString()
                blockDateText = getBlockDateText(estimatedHeight)
                rescanButtonEnabled = isValid && isDifferent
                datePickerLoading = false
                closeDatePicker = true
            } else {
                datePickerLoading = false
                closeDatePicker = true
            }
            emitState()
        }
    }

    private suspend fun estimateBlockHeightFromDate(day: Int, month: Int, year: Int): Long? = withContext(Dispatchers.Default) {

        delay(2000)
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, month - 1)
            set(java.util.Calendar.DAY_OF_MONTH, day)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val selectedDate = Date(calendar.timeInMillis)

        when (blockchainType) {
            BlockchainType.Zcash -> {
                ZcashAdapter.estimateBirthdayHeight(App.instance, selectedDate).also {
                    Log.e("eee", "?????????? estimated birthdayHeight: $it")
                }
            }
            BlockchainType.Monero -> {
                MoneroKit.restoreHeightForDate(selectedDate)
            }
            else -> null
        }
    }

    private fun getBlockDateText(height: Long?): String? {
        if (height == null) return null

        // Estimate block date based on blockchain type
        val estimatedDate = estimateBlockDate(height)
        return estimatedDate?.let { DateHelper.formatDate(it, "MMM d, yyyy") }
    }

    private fun estimateBlockDate(height: Long): Date? {
        val now = System.currentTimeMillis()

        return when (blockchainType) {
            BlockchainType.Zcash -> {
                // Zcash: ~75 seconds per block
                // Genesis: Oct 28, 2016
                val genesisTimestamp = 1477656000000L
                val blockTime = 75 * 1000L
                val estimatedTimestamp = genesisTimestamp + (height * blockTime)
                if (estimatedTimestamp <= now) Date(estimatedTimestamp) else Date(now)
            }
            BlockchainType.Monero -> {
                MoneroKit.dateForRestoreHeight(height)
            }
            else -> null
        }
    }
}
