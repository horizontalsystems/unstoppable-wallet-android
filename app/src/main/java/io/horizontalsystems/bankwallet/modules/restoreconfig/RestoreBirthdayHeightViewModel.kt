package io.horizontalsystems.bankwallet.modules.restoreconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingType
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.BirthdayHeightConfig
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.monerokit.MoneroKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class RestoreBirthdayHeightViewModel(
    private val blockchainType: BlockchainType
) : ViewModelUiState<RestoreBirthdayHeightUiState>() {

    private val minBirthdayHeight: Long = when (blockchainType) {
        BlockchainType.Zcash -> 420_000L
        BlockchainType.Monero -> 0L
        else -> 0L
    }

    private val maxBirthdayHeight: Long = UInt.MAX_VALUE.toLong()

    private val datePickerYears: List<Int> = run {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val startYear = when (blockchainType) {
            BlockchainType.Zcash -> 2018
            BlockchainType.Monero -> 2014
            else -> 2009
        }
        (startYear..currentYear).toList()
    }

    private val defaultBirthdayHeight: Long? = App.restoreSettingsManager
        .getSettingValueForCreatedAccount(RestoreSettingType.BirthdayHeight, blockchainType)
        ?.toLongOrNull()

    private var birthdayHeight: Long? = null
    private var birthdayHeightText: String? = null
    private var blockDateText: String? = null
    private var cachedEstimatedDate: Date? = null
    private var doneButtonEnabled: Boolean = true
    private var datePickerLoading: Boolean = false
    private var closeDatePicker: Boolean = false
    private var closeWithResult: BirthdayHeightConfig? = null

    val hintText: String = defaultBirthdayHeight?.toString() ?: ""

    init {
        updateBlockDateText(defaultBirthdayHeight)
    }

    override fun createState() = RestoreBirthdayHeightUiState(
        birthdayHeight = birthdayHeight,
        birthdayHeightText = birthdayHeightText,
        blockDateText = blockDateText,
        doneButtonEnabled = doneButtonEnabled,
        datePickerLoading = datePickerLoading,
        closeDatePicker = closeDatePicker,
        closeWithResult = closeWithResult,
        datePickerYears = datePickerYears
    )

    fun setBirthdayHeight(heightText: String) {
        val height = heightText.toLongOrNull()
        val isValid = height == null || (height in minBirthdayHeight..maxBirthdayHeight)

        birthdayHeight = height
        birthdayHeightText = null
        doneButtonEnabled = isValid
        // Show block date for entered value, or default if empty
        updateBlockDateText(height ?: defaultBirthdayHeight)
    }

    fun onDoneClick() {
        val heightToUse = birthdayHeight ?: defaultBirthdayHeight
        closeWithResult = BirthdayHeightConfig(heightToUse?.toString(), heightToUse == null)
        emitState()
    }

    fun onClosed() {
        closeWithResult = null
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
        val date = cachedEstimatedDate ?: Date()
        val calendar = Calendar.getInstance().apply { time = date }
        return Triple(
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.YEAR)
        )
    }

    fun onDateSelected(day: Int, month: Int, year: Int) {
        datePickerLoading = true
        emitState()

        viewModelScope.launch {
            val estimatedHeight = estimateBlockHeightFromDate(day, month, year)
            if (estimatedHeight != null) {
                val isValid = estimatedHeight in minBirthdayHeight..maxBirthdayHeight

                birthdayHeight = estimatedHeight
                birthdayHeightText = estimatedHeight.toString()
                doneButtonEnabled = isValid
                datePickerLoading = false
                closeDatePicker = true
                emitState()
                updateBlockDateText(estimatedHeight)
            } else {
                datePickerLoading = false
                closeDatePicker = true
                emitState()
            }
        }
    }

    private suspend fun estimateBlockHeightFromDate(day: Int, month: Int, year: Int): Long? = withContext(Dispatchers.Default) {
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

    private fun updateBlockDateText(height: Long?) {
        if (height == null) {
            blockDateText = null
            cachedEstimatedDate = null
            emitState()
            return
        }

        blockDateText = ""
        emitState()

        viewModelScope.launch {
            val heightForEstimate = if (height < minBirthdayHeight) minBirthdayHeight else height
            val estimatedDate = estimateBlockDate(heightForEstimate)
            val currentDate = Date()
            if (estimatedDate != null && estimatedDate.after(currentDate)) {
                cachedEstimatedDate = currentDate
                blockDateText = DateHelper.formatDate(currentDate, "MMM d, yyyy")
                doneButtonEnabled = false
            } else {
                cachedEstimatedDate = estimatedDate
                blockDateText = estimatedDate?.let { DateHelper.formatDate(it, "MMM d, yyyy") }
            }
            emitState()
        }
    }

    private suspend fun estimateBlockDate(height: Long): Date? = withContext(Dispatchers.IO) {
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

    class Factory(
        private val blockchainType: BlockchainType
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RestoreBirthdayHeightViewModel(blockchainType) as T
        }
    }
}

data class RestoreBirthdayHeightUiState(
    val birthdayHeight: Long? = null,
    val birthdayHeightText: String? = null,
    val blockDateText: String? = null,
    val doneButtonEnabled: Boolean = true,
    val datePickerLoading: Boolean = false,
    val closeDatePicker: Boolean = false,
    val closeWithResult: BirthdayHeightConfig? = null,
    val datePickerYears: List<Int> = emptyList()
)
