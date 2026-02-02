package io.horizontalsystems.bankwallet.modules.restoreconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.BirthdayHeightHelper
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingType
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.BirthdayHeightConfig
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch
import java.util.Date

class RestoreBirthdayHeightViewModel(
    private val blockchainType: BlockchainType
) : ViewModelUiState<RestoreBirthdayHeightUiState>() {

    private val minBirthdayHeight = BirthdayHeightHelper.minBirthdayHeight(blockchainType)
    private val datePickerYears = BirthdayHeightHelper.datePickerYears(blockchainType)

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
        val isValid = height == null || BirthdayHeightHelper.isHeightValid(blockchainType, height)

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
        return BirthdayHeightHelper.getInitialDateForPicker(cachedEstimatedDate)
    }

    fun onDateSelected(day: Int, month: Int, year: Int) {
        datePickerLoading = true
        emitState()

        viewModelScope.launch {
            val estimatedHeight = BirthdayHeightHelper.estimateBlockHeightFromDate(blockchainType, day, month, year)
            if (estimatedHeight != null) {
                val isValid = BirthdayHeightHelper.isHeightValid(blockchainType, estimatedHeight)

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
            val estimatedDate = BirthdayHeightHelper.estimateBlockDate(blockchainType, heightForEstimate)
            val currentDate = Date()

            if (BirthdayHeightHelper.isDateInFuture(estimatedDate)) {
                cachedEstimatedDate = currentDate
                blockDateText = BirthdayHeightHelper.formatBlockDate(currentDate)
                doneButtonEnabled = false
            } else {
                cachedEstimatedDate = estimatedDate
                blockDateText = estimatedDate?.let { BirthdayHeightHelper.formatBlockDate(it) }
            }
            emitState()
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
