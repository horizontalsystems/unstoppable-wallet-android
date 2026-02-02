package io.horizontalsystems.bankwallet.modules.balance.token

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.core.managers.BirthdayHeightHelper
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.marketkit.models.BlockchainType
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

    private val minBirthdayHeight = BirthdayHeightHelper.minBirthdayHeight(blockchainType)
    private val datePickerYears = BirthdayHeightHelper.datePickerYears(blockchainType)

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
        closeDatePicker = closeDatePicker,
        datePickerYears = datePickerYears
    )

    fun setBirthdayHeight(heightText: String) {
        val height = heightText.toLongOrNull()
        val isHeightValid = BirthdayHeightHelper.isHeightValid(blockchainType, height)
        val isDifferent = height != currentBirthdayHeight

        // When input is cleared, show the current birthday height's date
        val dateHeight = height ?: currentBirthdayHeight

        birthdayHeight = height
        birthdayHeightText = null // Reset so LaunchedEffect can trigger again for same value
        rescanButtonEnabled = isHeightValid && isDifferent
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
        return BirthdayHeightHelper.getInitialDateForPicker(cachedEstimatedDate)
    }

    fun onDateSelected(day: Int, month: Int, year: Int) {
        datePickerLoading = true
        emitState()

        viewModelScope.launch {
            val estimatedHeight = BirthdayHeightHelper.estimateBlockHeightFromDate(blockchainType, day, month, year)
            if (estimatedHeight != null) {
                val isValid = BirthdayHeightHelper.isHeightValid(blockchainType, estimatedHeight)
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
                rescanButtonEnabled = false
            } else {
                cachedEstimatedDate = estimatedDate
                blockDateText = estimatedDate?.let { BirthdayHeightHelper.formatBlockDate(it) }
            }
            emitState()
        }
    }
}
