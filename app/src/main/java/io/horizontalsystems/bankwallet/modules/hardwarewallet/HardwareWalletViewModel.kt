package io.horizontalsystems.bankwallet.modules.hardwarewallet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.hdwalletkit.HDExtendedKey

class HardwareWalletViewModel(
    private val hardwareWalletService: HardwareWalletService
) : ViewModel() {

    private var accountCreated = false
    private var submitButtonType: SubmitButtonType = SubmitButtonType.Next(false)
    private var type = Type.EvmAddressHardware
    private var address: Address? = null
    private var xPubKey: String? = null
    private var invalidXPubKey = false

    private var accountType: AccountType? = null
    private var accountNameEdited = false
    val defaultAccountName = hardwareWalletService.nextHardwareAccountName()
    var accountName: String = defaultAccountName
        get() = field.ifBlank { defaultAccountName }
        private set


    var uiState by mutableStateOf(
        HardwareWalletUiState(
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
        uiState = HardwareWalletUiState(
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

    fun onClickDone() {
        try {
            val accountType = getAccountType() ?: throw Exception()

            hardwareWalletService.hardwareAll(accountType, accountName)

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
            Type.EvmAddressHardware    -> SubmitButtonType.Next(address != null)
            //Type.XPubKeyHardware       -> SubmitButtonType.Next(xPubKey != null)
            //Type.SolanaAddressHardware -> SubmitButtonType.Done(address != null)
            //Type.TronAddressHardware   -> SubmitButtonType.Done(address != null)
        }
    }

    private fun getAccountType() = when (type) {
        Type.EvmAddressHardware    -> address?.let { AccountType.EvmAddressHardware(it.hex) }
        //Type.SolanaAddressHardware -> address?.let { AccountType.SolanaAddressHardware(it.hex) }
        //Type.TronAddressHardware   -> address?.let { AccountType.TronAddressHardware(it.hex)}
        //Type.XPubKeyHardware       -> xPubKey?.let { AccountType.HdExtendedKeyHardware(it) }
    }

    enum class Type(val titleResId: Int, val subtitleResId: Int) {
        EvmAddressHardware(R.string.Watch_TypeEvmAddress, R.string.Watch_TypeEvmAddress_Subtitle),
        //TronAddressHardware(R.string.Watch_TypeTronAddress, R.string.Watch_TypeTronAddress_Subtitle),
        //SolanaAddressHardware(R.string.Watch_TypeSolanaAddress, R.string.Watch_TypeSolanaAddress_Subtitle),
        //XPubKeyHardware(R.string.Watch_TypeXPubKey, R.string.Watch_TypeXPubKey_Subtitle),
    }
}

data class HardwareWalletUiState(
    val accountCreated: Boolean,
    val submitButtonType: SubmitButtonType,
    val type: HardwareWalletViewModel.Type,
    val accountType: AccountType?,
    val accountName: String?,
    val invalidXPubKey: Boolean
)

sealed class SubmitButtonType {
    data class Done(val enabled: Boolean) : SubmitButtonType()
    data class Next(val enabled: Boolean) : SubmitButtonType()
}
