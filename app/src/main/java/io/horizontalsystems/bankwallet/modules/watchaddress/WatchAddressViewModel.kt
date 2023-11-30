package io.horizontalsystems.bankwallet.modules.watchaddress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.BitcoinAddress
import io.horizontalsystems.bankwallet.entities.tokenType
import io.horizontalsystems.bankwallet.modules.address.AddressParserChain
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.marketkit.models.BlockchainType

class WatchAddressViewModel(
    private val watchAddressService: WatchAddressService,
    private val addressParserChain: AddressParserChain
) : ViewModel() {

    private var accountCreated = false
    private var submitButtonType: SubmitButtonType = SubmitButtonType.Next(false)
    private var type = Type.Unsupported
    private var address: Address? = null
    private var xPubKey: String? = null
    private var invalidInput = false
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
            accountType = accountType,
            accountName = accountName,
            invalidInput = invalidInput
        )
    )
        private set

    private fun emitState() {
        uiState = WatchAddressUiState(
            accountCreated = accountCreated,
            submitButtonType = submitButtonType,
            accountType = accountType,
            accountName = accountName,
            invalidInput = invalidInput
        )
    }

    fun onEnterAccountName(v: String) {
        accountNameEdited = v.isNotBlank()
        accountName = v
    }

    fun onEnterAddress(v: String) {
        if (v.isBlank()) {
            address = null
            xPubKey = null
            invalidInput = false

            syncSubmitButtonType()
            emitState()
        } else {
            val address = addressParserChain.parse(v)
            if (address != null) {
                onEnterAddress(address)
            } else {
                onEnterXPubKey(v)
            }
        }
    }

    private fun onEnterAddress(address: Address) {
        this.address = address
        if (!accountNameEdited) {
            accountName = address.domain ?: defaultAccountName
        }

        type = addressType(address)
        invalidInput = type == Type.Unsupported

        syncSubmitButtonType()
        emitState()
    }

    private fun addressType(address: Address) = when (address.blockchainType) {
        BlockchainType.Bitcoin,
        BlockchainType.BitcoinCash,
        BlockchainType.ECash,
        BlockchainType.Litecoin,
        BlockchainType.Dash -> Type.BitcoinAddress

        BlockchainType.Ethereum,
        BlockchainType.BinanceSmartChain,
        BlockchainType.BinanceChain,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.ArbitrumOne,
        BlockchainType.Gnosis,
        BlockchainType.Fantom -> Type.EvmAddress

        BlockchainType.Solana -> Type.SolanaAddress
        BlockchainType.Tron -> Type.TronAddress
        BlockchainType.Ton -> Type.TonAddress

        BlockchainType.Zcash,
        is BlockchainType.Unsupported,
        null -> Type.Unsupported
    }

    private fun onEnterXPubKey(input: String) {
        xPubKey = try {
            val hdKey = HDExtendedKey(input)
            require(hdKey.isPublic) {
                throw HDExtendedKey.ParsingError.WrongVersion
            }

            invalidInput = false
            type = Type.XPubKey

            input
        } catch (t: Throwable) {

            invalidInput = input.isNotBlank()
            type = Type.Unsupported

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

    private fun syncSubmitButtonType() {
        submitButtonType = when (type) {
            Type.EvmAddress -> SubmitButtonType.Next(address != null)
            Type.XPubKey -> SubmitButtonType.Next(xPubKey != null)
            Type.SolanaAddress -> SubmitButtonType.Watch(address != null)
            Type.TronAddress -> SubmitButtonType.Watch(address != null)
            Type.BitcoinAddress -> SubmitButtonType.Watch(address != null)
            Type.TonAddress -> SubmitButtonType.Watch(address != null)
            Type.Unsupported -> SubmitButtonType.Watch(false)
        }
    }

    private fun getAccountType() = when (type) {
        Type.EvmAddress -> address?.let { AccountType.EvmAddress(it.hex) }
        Type.SolanaAddress -> address?.let { AccountType.SolanaAddress(it.hex) }
        Type.TronAddress -> address?.let { AccountType.TronAddress(it.hex) }
        Type.XPubKey -> xPubKey?.let { AccountType.HdExtendedKey(it) }
        Type.BitcoinAddress -> address?.let {
            if (it is BitcoinAddress) {
                AccountType.BitcoinAddress(address = it.hex, blockchainType = it.blockchainType!!, tokenType = it.tokenType)
            } else {
                throw IllegalStateException("Unsupported address type")
            }
        }
        Type.TonAddress -> address?.let {
            AccountType.TonAddress(it.hex)
        }

        Type.Unsupported -> throw IllegalStateException("Unsupported address type")
    }

    enum class Type {
        EvmAddress,
        TronAddress,
        SolanaAddress,
        XPubKey,
        BitcoinAddress,
        TonAddress,
        Unsupported
    }
}

data class WatchAddressUiState(
    val accountCreated: Boolean,
    val submitButtonType: SubmitButtonType,
    val accountType: AccountType?,
    val accountName: String?,
    val invalidInput: Boolean
)

sealed class SubmitButtonType {
    data class Watch(val enabled: Boolean) : SubmitButtonType()
    data class Next(val enabled: Boolean) : SubmitButtonType()
}
