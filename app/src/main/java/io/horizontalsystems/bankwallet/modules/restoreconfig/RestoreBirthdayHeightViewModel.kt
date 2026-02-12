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
import java.time.LocalDate
import java.util.Date

class RestoreBirthdayHeightViewModel(
    private val blockchainType: BlockchainType
) : ViewModelUiState<RestoreBirthdayHeightUiState>() {

    private val minBirthdayHeight = BirthdayHeightHelper.minBirthdayHeight(blockchainType)
    private val firstBlockDate = BirthdayHeightHelper.getFirstBlockDate(blockchainType)

    private val defaultBirthdayHeight: Long? = App.restoreSettingsManager
        .getSettingValueForCreatedAccount(RestoreSettingType.BirthdayHeight, blockchainType)
        ?.toLongOrNull()

    private var birthdayHeight: Long? = null
    private var birthdayHeightText: String? = null
    private var blockDateText: String? = null
    private var cachedEstimatedDate: Date? = null
    private var doneButtonEnabled: Boolean = true
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
        closeWithResult = closeWithResult,
        firstBlockDate = firstBlockDate
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

    fun getInitialDateForPicker(): Triple<Int, Int, Int> {
        return BirthdayHeightHelper.getInitialDateForPicker(cachedEstimatedDate)
    }

    suspend fun onDateSelected(day: Int, month: Int, year: Int) {
        val estimatedHeight = BirthdayHeightHelper.estimateBlockHeightFromDate(blockchainType, day, month, year)
        if (estimatedHeight != null) {
            val isValid = BirthdayHeightHelper.isHeightValid(blockchainType, estimatedHeight)

            birthdayHeight = estimatedHeight
            birthdayHeightText = estimatedHeight.toString()
            doneButtonEnabled = isValid
            emitState()
            updateBlockDateText(estimatedHeight)
        } else {
            updateBlockDateText(null)
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
    val closeWithResult: BirthdayHeightConfig? = null,
    val firstBlockDate: LocalDate? = null
)
