package io.horizontalsystems.bankwallet.modules.balance.token

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.monerokit.MoneroKit
import java.util.Date

class EnterBirthdayHeightViewModel(
    private val blockchainType: BlockchainType,
    private val account: Account,
    private val currentBirthdayHeight: Long?,
    private val restoreSettingsManager: RestoreSettingsManager
) : ViewModel() {

    var uiState by mutableStateOf(
        EnterBirthdayHeightModule.UiState(
            birthdayHeight = null,
            blockDateText = getBlockDateText(currentBirthdayHeight),
            rescanButtonEnabled = false,
            closeAfterRescan = false
        )
    )
        private set

    fun setBirthdayHeight(heightText: String) {
        val height = heightText.toLongOrNull()
        val isValid = height != null && height > 0
        val isDifferent = height != currentBirthdayHeight

        // When input is cleared, show the current birthday height's date
        val dateHeight = height ?: currentBirthdayHeight

        uiState = uiState.copy(
            birthdayHeight = height,
            blockDateText = getBlockDateText(dateHeight),
            rescanButtonEnabled = isValid && isDifferent
        )
    }

    fun onRescanClick() {
        val newHeight = uiState.birthdayHeight ?: return

        val settings = RestoreSettings()
        settings.birthdayHeight = newHeight
        restoreSettingsManager.save(settings, account, blockchainType)

        uiState = uiState.copy(closeAfterRescan = true)
    }

    fun onRescanHandled() {
        uiState = uiState.copy(closeAfterRescan = false)
    }

    fun onDatePickerOpened() {
        uiState = uiState.copy(datePickerLoading = false)
    }

    fun getInitialDateForPicker(): Triple<Int, Int, Int> {
        val height = uiState.birthdayHeight ?: currentBirthdayHeight
        val date = height?.let { estimateBlockDate(it) } ?: Date()
        val calendar = java.util.Calendar.getInstance().apply { time = date }
        return Triple(
            calendar.get(java.util.Calendar.DAY_OF_MONTH),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.YEAR)
        )
    }

    fun estimateBlockHeightFromDate(day: Int, month: Int, year: Int): Long? {
        uiState = uiState.copy(datePickerLoading = true)

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
        val now = System.currentTimeMillis()

        val height = when (blockchainType) {
            BlockchainType.Zcash -> {
                // Zcash: ~75 seconds per block
                // Genesis: Oct 28, 2016
                val genesisTimestamp = 1477656000000L
                val blockTime = 75 * 1000L
                val selectedTimestamp = calendar.timeInMillis
                if (selectedTimestamp <= genesisTimestamp) {
                    1L
                } else {
                    val timeSinceGenesis = (minOf(selectedTimestamp, now) - genesisTimestamp)
                    (timeSinceGenesis / blockTime).coerceAtLeast(1L)
                }
            }
            BlockchainType.Monero -> {
                MoneroKit.restoreHeightForDate(selectedDate)
            }
            else -> null
        }

        uiState = uiState.copy(datePickerLoading = false)
        return height
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
