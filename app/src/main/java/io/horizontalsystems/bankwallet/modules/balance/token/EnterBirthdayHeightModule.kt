package io.horizontalsystems.bankwallet.modules.balance.token

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.marketkit.models.BlockchainType

object EnterBirthdayHeightModule {

    class Factory(
        private val blockchainType: BlockchainType,
        private val account: Account,
        private val currentBirthdayHeight: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EnterBirthdayHeightViewModel(
                blockchainType = blockchainType,
                account = account,
                currentBirthdayHeight = currentBirthdayHeight,
                restoreSettingsManager = App.restoreSettingsManager,
                adapterManager = App.adapterManager,
                walletManager = App.walletManager
            ) as T
        }
    }

    data class UiState(
        val birthdayHeight: Long? = null,
        val birthdayHeightText: String? = null,
        val blockDateText: String? = null,
        val rescanButtonEnabled: Boolean = false,
        val rescanLoading: Boolean = false,
        val closeAfterRescan: Boolean = false,
        val datePickerLoading: Boolean = false,
        val closeDatePicker: Boolean = false
    )
}
