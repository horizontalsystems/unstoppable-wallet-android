package io.horizontalsystems.bankwallet.modules.watchaddress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.hdwalletkit.HDExtendedKey

class WatchAddressViewModel(
    private val watchAddressService: WatchAddressService
) : ViewModel() {

    private var accountCreated = false
    private var submitButtonType: SubmitButtonType = SubmitButtonType.Next(false)
    private var type = Type.EvmAddress
    private var address: Address? = null
    private var xPubKey: String? = null
    private var invalidXPubKey = false

    private var accountType: AccountType? = null
    private var accountNameEdited = false
    val defaultAccountName = watchAddressService.nextWatchAccountName()
    var accountName: String = defaultAccountName
        get() = field.ifBlank { defaultAccountName }
        private set


    var uiState by mutableStateOf(
        WatchAddressUiState(
            accountCreated = accountCreated,
            submitButtonType = submitButtonType,
            type = type,
            accountType = accountType,
            accountName = accountName,
            invalidXPubKey = invalidXPubKey
        )
    )
        private set

    private fun emitState() {
        uiState = WatchAddressUiState(
            accountCreated = accountCreated,
            submitButtonType = submitButtonType,
            type = type,
            accountType = accountType,
            accountName = accountName,
            invalidXPubKey = invalidXPubKey
        )
    }

    fun onEnterAccountName(v: String) {
        accountNameEdited = v.isNotBlank()
        accountName = v
    }

    fun onEnterAddress(v: Address?) {
        address = v
        if (!accountNameEdited) {
            accountName = v?.domain ?: defaultAccountName
        }

        syncSubmitButtonType()
        emitState()
    }

    fun onEnterXPubKey(v: String) {
        xPubKey = try {
            val hdKey = HDExtendedKey(v)
            require(hdKey.isPublic) {
                throw HDExtendedKey.ParsingError.WrongVersion
            }
            invalidXPubKey = false
            v
        } catch (t: Throwable) {
            invalidXPubKey = v.isNotBlank()
            null
        }

        syncSubmitButtonType()
        emitState()
    }

    fun blockchainSelectionOpened() {
        accountType = null

        emitState()
    }

    fun onClickNext() {
        accountType = getAccountType()

        emitState()
    }

    fun onClickWatch() {
        try {
            val accountType = getAccountType() ?: throw Exception()

            watchAddressService.watchAll(accountType, accountName)

            accountCreated = true
            emitState()
        } catch (_: Exception) {

        }
    }

    fun onSetType(type: Type) {
        this.type = type

        address = null
        xPubKey = null

        if (!accountNameEdited) {
            accountName = defaultAccountName
        }

        syncSubmitButtonType()
        emitState()
    }

    private fun syncSubmitButtonType() {
        submitButtonType = when (type) {
            Type.EvmAddress -> SubmitButtonType.Next(address != null)
            Type.XPubKey -> SubmitButtonType.Next(xPubKey != null)
            Type.SolanaAddress -> SubmitButtonType.Watch(address != null)
            Type.TronAddress -> SubmitButtonType.Watch(address != null)
        }
    }

    private fun getAccountType() = when (type) {
        Type.EvmAddress -> address?.let { AccountType.EvmAddress(it.hex) }
        Type.SolanaAddress -> address?.let { AccountType.SolanaAddress(it.hex) }
        Type.TronAddress -> address?.let { AccountType.TronAddress(it.hex)}
        Type.XPubKey -> xPubKey?.let { AccountType.HdExtendedKey(it) }
    }

    enum class Type(val titleResId: Int) {
        EvmAddress(R.string.Watch_TypeEvmAddress),
        TronAddress(R.string.Watch_TypeTronAddress),
        SolanaAddress(R.string.Watch_TypeSolanaAddress),
        XPubKey(R.string.Watch_TypeXPubKey),
    }
}

data class WatchAddressUiState(
    val accountCreated: Boolean,
    val submitButtonType: SubmitButtonType,
    val type: WatchAddressViewModel.Type,
    val accountType: AccountType?,
    val accountName: String?,
    val invalidXPubKey: Boolean
)

sealed class SubmitButtonType {
    data class Watch(val enabled: Boolean) : SubmitButtonType()
    data class Next(val enabled: Boolean) : SubmitButtonType()
}
