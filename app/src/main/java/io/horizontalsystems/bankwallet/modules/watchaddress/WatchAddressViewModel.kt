package io.horizontalsystems.bankwallet.modules.watchaddress

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statAccountType
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.BitcoinAddress
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.MoneroWatchAddress
import io.horizontalsystems.bankwallet.entities.tokenType
import io.horizontalsystems.bankwallet.modules.address.AddressParserChain
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.monerokit.MoneroKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchAddressViewModel(
    private val watchAddressService: WatchAddressService,
    private val addressParserChain: AddressParserChain
) : ViewModelUiState<WatchAddressUiState>() {

    private var accountCreated = false
    private var submitButtonType: SubmitButtonType = SubmitButtonType.Next(false)
    private var type = Type.Unsupported
    private var address: Address? = null
    private var viewKey: String? = null
    private var birthdayHeight: Long? = null
    private var xPubKey: String? = null
    private var accountType: AccountType? = null
    private var accountNameEdited = false
    private var inputState: DataState<String>? = null
    private var viewKeyState: DataState<String>? = null
    private var birthdayHeightState: DataState<String>? = null
    private var parseAddressJob: Job? = null

    val defaultAccountName = watchAddressService.nextWatchAccountName()
    var accountName: String = defaultAccountName
        get() = field.ifBlank { defaultAccountName }
        private set

    override fun createState() = WatchAddressUiState(
        accountCreated = accountCreated,
        submitButtonType = submitButtonType,
        accountType = accountType,
        accountName = accountName,
        inputState = inputState,
        addressType = type,
        viewKeyState = viewKeyState,
        birthdayHeightState = birthdayHeightState
    )

    fun onEnterAccountName(v: String) {
        accountNameEdited = v.isNotBlank()
        accountName = v
    }

    fun onEnterInput(v: String) {
        parseAddressJob?.cancel()
        address = null
        xPubKey = null
        type = Type.Unsupported
        viewKey = null
        birthdayHeight = null
        viewKeyState = null
        birthdayHeightState = null

        if (v.isBlank()) {
            inputState = null
            if (!accountNameEdited) {
                accountName = defaultAccountName
            }
            syncSubmitButtonType()
            emitState()
        } else {
            inputState = DataState.Loading
            syncSubmitButtonType()
            emitState()

            val vTrimmed = v.trim()
            parseAddressJob = viewModelScope.launch(Dispatchers.IO) {
                val handler = addressParserChain.supportedHandler(vTrimmed)

                if (handler == null) {
                    ensureActive()
                    withContext(Dispatchers.Main) {
                        setXPubKey(vTrimmed)
                    }
                    return@launch
                } else {
                    try {
                        val parsedAddress = handler.parseAddress(vTrimmed)
                        ensureActive()
                        withContext(Dispatchers.Main) {
                            if (parsedAddress is MoneroWatchAddress) {
                                setAddress(parsedAddress)
                                onEnterViewKey(parsedAddress.viewKey)
                                parsedAddress.height?.let {
                                    onEnterBirthdayHeight(parsedAddress.height.toString())
                                }
                            } else {
                                setAddress(parsedAddress)
                            }
                        }
                    } catch (t: Throwable) {
                        ensureActive()
                        withContext(Dispatchers.Main) {
                            inputState = DataState.Error(t)
                            syncSubmitButtonType()
                            emitState()
                        }
                    }
                }
            }
        }
    }

    fun onEnterViewKey(key: String) {
        val address = address
        if (address == null) {
            inputState = DataState.Error(UnsupportedAddress)
            syncSubmitButtonType()
            emitState()
            return
        }

        if (key.trim().isEmpty()) {
            viewKey = null
            viewKeyState = null
            syncSubmitButtonType()
            emitState()
            return
        }

        viewKeyState = DataState.Loading
        emitState()

        try {
            MoneroKit.validatePrivateViewKey(key, address.hex)
            viewKeyState = DataState.Success(key)
            viewKey = key
        } catch (ex: Exception) {
            viewKeyState = DataState.Error(ex)
        }

        syncSubmitButtonType()
        emitState()
    }


    fun onEnterBirthdayHeight(height: String) {
        if (height.trim().isEmpty()) {
            birthdayHeight = null
            birthdayHeightState = null
            syncSubmitButtonType()
            emitState()
            return
        }

        birthdayHeightState = DataState.Loading
        val convertedHeight = height.toLongOrNull()
        if (convertedHeight == null) {
            birthdayHeightState = DataState.Error(Exception(Translator.getString(R.string.Restore_BirthdayHeight_Invalid)))
        } else {
            birthdayHeightState = DataState.Success(height)
            birthdayHeight = convertedHeight
        }
        syncSubmitButtonType()
        emitState()
    }

    private fun setAddress(address: Address) {
        this.address = address
        if (!accountNameEdited) {
            accountName = address.domain ?: defaultAccountName
        }

        type = addressType(address)
        inputState = DataState.Success(address.domain ?: address.hex)

        syncSubmitButtonType()
        emitState()
    }

    private fun setXPubKey(input: String) {
        xPubKey = try {
            val hdKey = HDExtendedKey(input)
            require(hdKey.isPublic) {
                throw HDExtendedKey.ParsingError.WrongVersion
            }

            inputState = DataState.Success(input)
            type = Type.XPubKey

            input
        } catch (t: Throwable) {
            inputState = DataState.Error(UnsupportedAddress)
            type = Type.Unsupported

            null
        }

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
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Optimism,
        BlockchainType.Base,
        BlockchainType.ZkSync,
        BlockchainType.ArbitrumOne,
        BlockchainType.Gnosis,
        BlockchainType.Fantom -> Type.EvmAddress

        BlockchainType.Solana -> Type.SolanaAddress
        BlockchainType.Tron -> Type.TronAddress
        BlockchainType.Ton -> Type.TonAddress
        BlockchainType.Stellar -> Type.StellarAddress
        BlockchainType.Monero -> Type.MoneroAddress
        BlockchainType.Zcash,
        is BlockchainType.Unsupported,
        null -> Type.Unsupported
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

            stat(
                page = StatPage.WatchWallet,
                event = StatEvent.WatchWallet(accountType.statAccountType)
            )
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
            Type.StellarAddress -> SubmitButtonType.Watch(address != null)
            Type.MoneroAddress -> SubmitButtonType.Watch(address != null && viewKey != null)
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

        Type.StellarAddress -> address?.let {
            AccountType.StellarAddress(it.hex)
        }

        Type.MoneroAddress -> address?.let {
            AccountType.MoneroWatchAccount(it.hex, viewKey!!, birthdayHeight ?: 1)
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
        StellarAddress,
        MoneroAddress,
        Unsupported
    }
}

data class WatchAddressUiState(
    val accountCreated: Boolean,
    val submitButtonType: SubmitButtonType,
    val accountType: AccountType?,
    val accountName: String?,
    val inputState: DataState<String>?,
    val addressType: WatchAddressViewModel.Type,
    val viewKeyState: DataState<String>?,
    val birthdayHeightState: DataState<String>?
)

sealed class SubmitButtonType {
    data class Watch(val enabled: Boolean) : SubmitButtonType()
    data class Next(val enabled: Boolean) : SubmitButtonType()
}

object UnsupportedAddress : Exception(Translator.getString(R.string.Watch_Error_InvalidAddressFormat))
