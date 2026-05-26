package io.horizontalsystems.bankwallet.modules.restoreconfig

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.BirthdayHeightHelper
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingType
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.BirthdayHeightConfig
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Date

@HiltViewModel(assistedFactory = RestoreBirthdayHeightViewModel.Factory::class)
class RestoreBirthdayHeightViewModel @AssistedInject constructor(
    @Assisted private val blockchainType: BlockchainType,
    restoreSettingsManager: RestoreSettingsManager,
) : ViewModelUiState<RestoreBirthdayHeightUiState>() {

    @AssistedFactory
    interface Factory {
        fun create(blockchainType: BlockchainType): RestoreBirthdayHeightViewModel
    }

    private val minBirthdayHeight = BirthdayHeightHelper.minBirthdayHeight(blockchainType)
    private val firstBlockDate = BirthdayHeightHelper.getFirstBlockDate(blockchainType)

    private val defaultBirthdayHeight: Long? = restoreSettingsManager
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

    fun getInitialDateForPicker(): LocalDate {
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

}

data class RestoreBirthdayHeightUiState(
    val birthdayHeight: Long? = null,
    val birthdayHeightText: String? = null,
    val blockDateText: String? = null,
    val doneButtonEnabled: Boolean = true,
    val closeWithResult: BirthdayHeightConfig? = null,
    val firstBlockDate: LocalDate? = null
)
