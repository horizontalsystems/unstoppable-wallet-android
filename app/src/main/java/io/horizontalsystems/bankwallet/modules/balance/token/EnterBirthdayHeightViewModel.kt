package io.horizontalsystems.bankwallet.modules.balance.token

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.monerokit.MoneroKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class EnterBirthdayHeightViewModel(
    private val blockchainType: BlockchainType,
    private val account: Account,
    private val currentBirthdayHeight: Long?,
    private val restoreSettingsManager: RestoreSettingsManager,
    private val adapterManager: IAdapterManager,
    private val walletManager: IWalletManager
) : ViewModelUiState<EnterBirthdayHeightModule.UiState>() {

    private val minBirthdayHeight: Long = when (blockchainType) {
        BlockchainType.Zcash -> 420_000L
        BlockchainType.Monero -> 0L
        else -> 0L
    }

    private val maxBirthdayHeight: Long = UInt.MAX_VALUE.toLong()

    private var birthdayHeight: Long? = null
    private var birthdayHeightText: String? = null
    private var blockDateText: String? = null
    private var cachedEstimatedDate: Date? = null
    private var rescanButtonEnabled: Boolean = false
    private var rescanLoading: Boolean = false
    private var closeAfterRescan: Boolean = false
    private var datePickerLoading: Boolean = false
    private var closeDatePicker: Boolean = false

    init {
        updateBlockDateText(currentBirthdayHeight)
    }

    override fun createState() = EnterBirthdayHeightModule.UiState(
        birthdayHeight = birthdayHeight,
        birthdayHeightText = birthdayHeightText,
        blockDateText = blockDateText,
        rescanButtonEnabled = rescanButtonEnabled,
        rescanLoading = rescanLoading,
        closeAfterRescan = closeAfterRescan,
        datePickerLoading = datePickerLoading,
        closeDatePicker = closeDatePicker
    )

    fun setBirthdayHeight(heightText: String) {
        val height = heightText.toLongOrNull()
        val isValid = height != null && height >= minBirthdayHeight && height <= maxBirthdayHeight
        val isDifferent = height != currentBirthdayHeight

        // When input is cleared, show the current birthday height's date
        val dateHeight = height ?: currentBirthdayHeight

        birthdayHeight = height
        birthdayHeightText = null // Reset so LaunchedEffect can trigger again for same value
        rescanButtonEnabled = isValid && isDifferent
        updateBlockDateText(dateHeight)
    }

    fun onRescanClick() {
        val newHeight = birthdayHeight ?: return

        rescanLoading = true
        emitState()

        viewModelScope.launch {
            if (blockchainType == BlockchainType.Zcash) {
                val zcashWallet = walletManager.activeWallets.firstOrNull {
                    it.account == account && it.token.blockchainType == BlockchainType.Zcash
                }

                zcashWallet?.let { wallet ->
                    val adapter: ZcashAdapter? = adapterManager.getAdapterForWallet(wallet)
                    withContext(Dispatchers.IO) {
                        adapter?.stop()
                        ZcashAdapter.clear(account.id)
                    }
                }
            }

            val settings = RestoreSettings()
            settings.birthdayHeight = newHeight
            restoreSettingsManager.save(settings, account, blockchainType)

            rescanLoading = false
            closeAfterRescan = true
            emitState()
        }
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
        val date = cachedEstimatedDate ?: Date()
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
                val isValid = estimatedHeight in minBirthdayHeight..maxBirthdayHeight
                val isDifferent = estimatedHeight != currentBirthdayHeight

                birthdayHeight = estimatedHeight
                birthdayHeightText = estimatedHeight.toString()
                rescanButtonEnabled = isValid && isDifferent
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
            val estimatedDate = estimateBlockDate(height)
            cachedEstimatedDate = estimatedDate
            blockDateText = estimatedDate?.let { DateHelper.formatDate(it, "MMM d, yyyy") }
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
}
