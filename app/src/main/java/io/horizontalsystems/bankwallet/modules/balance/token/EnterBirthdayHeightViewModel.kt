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

    private fun getBlockDateText(height: Long?): String? {
        if (height == null) return null

        // Estimate block date based on blockchain type
        val estimatedDate = estimateBlockDate(height)
        return estimatedDate?.let { DateHelper.formatDate(it, "MMM d, yyyy") }
    }

    private fun estimateBlockDate(height: Long): Date? {
        // This is a simplified estimation. In reality, you'd want to use
        // checkpoint data or blockchain-specific calculation
        val now = System.currentTimeMillis()

        return when (blockchainType) {
            BlockchainType.Zcash -> {
                // Zcash: ~75 seconds per block
                // Genesis: Oct 28, 2016
                val genesisTimestamp = 1477656000000L // Oct 28, 2016
                val blockTime = 75 * 1000L // 75 seconds in milliseconds
                val estimatedTimestamp = genesisTimestamp + (height * blockTime)
                if (estimatedTimestamp <= now) Date(estimatedTimestamp) else Date(now)
            }
            BlockchainType.Monero -> {
                // Monero: ~120 seconds per block
                // Genesis: Apr 18, 2014
                val genesisTimestamp = 1397818193000L // Apr 18, 2014
                val blockTime = 120 * 1000L // 120 seconds in milliseconds
                val estimatedTimestamp = genesisTimestamp + (height * blockTime)
                if (estimatedTimestamp <= now) Date(estimatedTimestamp) else Date(now)
            }
            else -> null
        }
    }
}
