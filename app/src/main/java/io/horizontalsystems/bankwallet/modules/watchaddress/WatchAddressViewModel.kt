package io.horizontalsystems.bankwallet.modules.watchaddress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.hdwalletkit.HDExtendedKey

class WatchAddressViewModel(
    private val accountFactory: IAccountFactory,
    private val accountManager: IAccountManager,
) : ViewModel() {

    private var accountCreated = false
    private var submitEnabled = false
    private var type = Type.Address
    private var address: Address? = null
    private var xPubKey: String? = null

    var uiState by mutableStateOf(
        WatchAddressUiState(
            accountCreated = accountCreated,
            submitEnabled = submitEnabled,
            type = type,
        )
    )
        private set

    private fun emitState() {
        uiState = WatchAddressUiState(
            accountCreated = accountCreated,
            submitEnabled = submitEnabled,
            type = type,
        )
    }

    fun onEnterAddress(v: Address?) {
        address = v

        submitEnabled = calculateIfSubmitEnabled()
        emitState()
    }

    fun onEnterXPubKey(v: String) {
        xPubKey = try {
            HDExtendedKey.validate(v, true)
            v
        } catch (t: Throwable) {
            null
        }

        submitEnabled = calculateIfSubmitEnabled()
        emitState()
    }

    fun onClickWatch() {
        try {
            createAccount()
            accountCreated = true
            emitState()
        } catch (_: Exception) {

        }
    }

    fun onSetType(type: Type) {
        this.type = type

        address = null
        xPubKey = null

        submitEnabled = calculateIfSubmitEnabled()
        emitState()
    }

    private fun calculateIfSubmitEnabled() = when (this.type) {
        Type.Address -> address != null
        Type.XPubKey -> xPubKey != null
    }

    private fun createAccount() {
        var accountName: String? = null
        val accountType: AccountType

        when (type) {
            Type.Address -> {
                val tmpAddress = address ?: throw Exception()
                accountName = address?.domain

                accountType = AccountType.EvmAddress(tmpAddress.hex)
            }
            Type.XPubKey -> {
                val tmpXPubKey = xPubKey ?: throw Exception()

                accountType = AccountType.HdExtendedKey(tmpXPubKey)
            }
        }

        if (accountName == null) {
            accountName = accountFactory.getNextWatchAccountName()
        }

        val account = accountFactory.watchAccount(accountName, accountType)

        accountManager.save(account)
    }

    enum class Type(val titleResId: Int) {
        Address(R.string.Watch_TypeAddress),
        XPubKey(R.string.Watch_TypeXPubKey),
    }
}

data class WatchAddressUiState(
    val accountCreated: Boolean,
    val submitEnabled: Boolean,
    val type: WatchAddressViewModel.Type
)
