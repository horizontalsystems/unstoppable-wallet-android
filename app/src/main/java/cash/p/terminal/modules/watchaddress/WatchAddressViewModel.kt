package cash.p.terminal.modules.watchaddress

import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.BitcoinAddress
import cash.p.terminal.ui_compose.entities.DataState
import cash.p.terminal.entities.tokenType
import cash.p.terminal.modules.address.AddressParserChain
import cash.p.terminal.modules.address.ZCashUfvkParser
import cash.p.terminal.wallet.AccountType
import cash.z.ecc.android.sdk.CloseableSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.WalletInitMode
import cash.z.ecc.android.sdk.model.AccountImportSetup
import cash.z.ecc.android.sdk.model.AccountPurpose
import cash.z.ecc.android.sdk.model.UnifiedFullViewingKey
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import io.horizontalsystems.core.ViewModelUiState
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.hdwalletkit.HDExtendedKey
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
    private var xPubKey: String? = null
    private var ufvkKey: String? = null
    private var accountType: AccountType? = null
    private var accountNameEdited = false
    private var inputState: DataState<String>? = null
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
        inputState = inputState
    )

    fun onEnterAccountName(v: String) {
        accountNameEdited = v.isNotBlank()
        accountName = v
    }

    fun onEnterInput(v: String) {
        parseAddressJob?.cancel()
        address = null
        xPubKey = null
        ufvkKey = null

        if (v.isBlank()) {
            inputState = null
            accountName = defaultAccountName
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
                    if (ZCashUfvkParser.isUfvk(vTrimmed) && isValidUfvkKey(vTrimmed)) {
                        return@launch
                    }
                    withContext(Dispatchers.Main) {
                        setXPubKey(vTrimmed)
                    }
                    return@launch
                } else {
                    try {
                        val parsedAddress = handler.parseAddress(vTrimmed)
                        ensureActive()
                        withContext(Dispatchers.Main) {
                            setAddress(parsedAddress)
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

    private fun setAddress(address: Address) {
        this.address = address
        if (!accountNameEdited) {
            accountName = address.domain ?: defaultAccountName
        }

        type = addressType(address)
        inputState = DataState.Success(address.hex)

        syncSubmitButtonType()
        emitState()
    }

    private suspend fun isValidUfvkKey(input: String): Boolean {
        var synchronizer: CloseableSynchronizer? = null
        try {
            // Clear previous synchronizer
            Synchronizer.erase(
                appContext = App.instance,
                network = ZcashNetwork.Mainnet
            )

            synchronizer = Synchronizer.new(
                context = App.instance,
                zcashNetwork = ZcashNetwork.Mainnet,
                lightWalletEndpoint = LightWalletEndpoint(
                    host = "zec.rocks",
                    port = 443,
                    isSecure = true
                ),
                birthday = null,
                walletInitMode = WalletInitMode.ExistingWallet,
                setup = null
            )
            (synchronizer as Synchronizer).getAccounts().forEach {
                println("Account: ${it.ufvk}")
            }
            // Check first existing accounts
            if ((synchronizer as Synchronizer).getAccounts().find { it.ufvk == input } == null) {
                // Try to import account
                (synchronizer as Synchronizer).importAccountByUfvk(
                    AccountImportSetup(
                        accountName = "check_address",
                        keySource = "user input",
                        purpose = AccountPurpose.ViewOnly,
                        ufvk = UnifiedFullViewingKey(input)
                    )
                )
            }
            ufvkKey = input
            type = Type.ZcashUfvk
            inputState = DataState.Success(input)
            withContext(Dispatchers.Main) {
                syncSubmitButtonType()
                emitState()
            }
            return true
        } catch (t: Throwable) {
            t.printStackTrace()
            inputState = DataState.Error(UnsupportedAddress)
            type = Type.Unsupported
        } finally {
            synchronizer?.close()
        }
        return false
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
        BlockchainType.Dogecoin,
        BlockchainType.Cosanta,
        BlockchainType.PirateCash,
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

        BlockchainType.Zcash,
        BlockchainType.Monero,
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
            Type.ZcashUfvk -> SubmitButtonType.Watch(ufvkKey != null)
            Type.Unsupported -> SubmitButtonType.Watch(false)
        }
    }

    private fun getAccountType() = when (type) {
        Type.EvmAddress -> address?.let { AccountType.EvmAddress(it.hex) }
        Type.SolanaAddress -> address?.let { AccountType.SolanaAddress(it.hex) }
        Type.TronAddress -> address?.let { AccountType.TronAddress(it.hex) }
        Type.XPubKey -> xPubKey?.let { AccountType.HdExtendedKey(it) }
        Type.ZcashUfvk -> ufvkKey?.let { AccountType.ZCashUfvKey(it) }
        Type.BitcoinAddress -> address?.let {
            if (it is BitcoinAddress) {
                AccountType.BitcoinAddress(
                    address = it.hex,
                    blockchainType = it.blockchainType!!,
                    tokenType = it.tokenType
                )
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
        Unsupported,
        ZcashUfvk
    }
}

data class WatchAddressUiState(
    val accountCreated: Boolean,
    val submitButtonType: SubmitButtonType,
    val accountType: AccountType?,
    val accountName: String?,
    val inputState: DataState<String>?
)

sealed class SubmitButtonType {
    data class Watch(val enabled: Boolean) : SubmitButtonType()
    data class Next(val enabled: Boolean) : SubmitButtonType()
}

object UnsupportedAddress :
    Exception(cash.p.terminal.strings.helpers.Translator.getString(R.string.Watch_Error_InvalidAddressFormat))
