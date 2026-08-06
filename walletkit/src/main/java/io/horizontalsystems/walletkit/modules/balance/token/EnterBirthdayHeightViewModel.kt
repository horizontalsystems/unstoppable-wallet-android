package io.horizontalsystems.walletkit.modules.balance.token

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.managers.BirthdayHeightHelper
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.core.managers.RestoreSettingsManager
import io.horizontalsystems.walletkit.core.managers.WalletManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Date

class EnterBirthdayHeightViewModel(
    private val blockchainType: BlockchainType,
    private val account: Account,
    private val currentBirthdayHeight: Long?,
    private val restoreSettingsManager: RestoreSettingsManager,
    private val adapterManager: IAdapterManager,
    private val walletManager: WalletManager
) : ViewModelUiState<EnterBirthdayHeightModule.UiState>() {

    private val minBirthdayHeight = BirthdayHeightHelper.minBirthdayHeight(blockchainType)
    private val firstBlockDate = BirthdayHeightHelper.getFirstBlockDate(blockchainType)

    private var birthdayHeight: Long? = null
    private var birthdayHeightText: String? = null
    private var blockDateText: String? = null
    private var cachedEstimatedDate: Date? = null
    private var rescanButtonEnabled: Boolean = false
    private var rescanLoading: Boolean = false
    private var closeAfterRescan: Boolean = false

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
        firstBlockDate = firstBlockDate
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
            try {
                walletManager.activeWallets.firstOrNull {
                    it.account == account && it.token.blockchainType == blockchainType
                }?.let { wallet ->
                    ChainRegistry[blockchainType]?.prepareRescan(wallet)
                }

                val settings = RestoreSettings()
                settings.birthdayHeight = newHeight
                restoreSettingsManager.save(settings, account, blockchainType)

                closeAfterRescan = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Timber.e(e, "Rescan preparation failed")
            } finally {
                rescanLoading = false
                emitState()
            }
        }
    }

    fun onRescanHandled() {
        closeAfterRescan = false
        emitState()
    }

    fun getInitialDateForPicker(): LocalDate {
        return BirthdayHeightHelper.getInitialDateForPicker(cachedEstimatedDate)
    }

    suspend fun onDateSelected(day: Int, month: Int, year: Int) {
        val estimatedHeight = BirthdayHeightHelper.estimateBlockHeightFromDate(blockchainType, day, month, year)
        if (estimatedHeight != null) {
            val isValid = BirthdayHeightHelper.isHeightValid(blockchainType, estimatedHeight)
            val isDifferent = estimatedHeight != currentBirthdayHeight

            birthdayHeight = estimatedHeight
            birthdayHeightText = estimatedHeight.toString()
            rescanButtonEnabled = isValid && isDifferent
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
                rescanButtonEnabled = false
            } else {
                cachedEstimatedDate = estimatedDate
                blockDateText = estimatedDate?.let { BirthdayHeightHelper.formatBlockDate(it) }
            }
            emitState()
        }
    }
}
