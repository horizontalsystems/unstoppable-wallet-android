package io.horizontalsystems.bankwallet.modules.send.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.title
import io.horizontalsystems.bankwallet.core.utils.AddressUriParser
import io.horizontalsystems.bankwallet.core.utils.AddressUriResult
import io.horizontalsystems.bankwallet.core.utils.ToncoinUriParser
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerBase58
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerBech32
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerBinanceChain
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerBitcoinCash
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerEns
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerEvm
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerSolana
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerTon
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerTron
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerUdn
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerZcash
import io.horizontalsystems.bankwallet.modules.address.AddressParserChain
import io.horizontalsystems.bankwallet.modules.address.AddressValidationException
import io.horizontalsystems.bankwallet.modules.address.EnsResolverHolder
import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.dashkit.MainNetDash
import io.horizontalsystems.ecash.MainNetECash
import io.horizontalsystems.litecoinkit.MainNetLitecoin
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class EnterAddressViewModel(
    private val blockchainType: BlockchainType,
    private val addressUriParser: AddressUriParser,
    private val addressParserChain: AddressParserChain
) : ViewModelUiState<EnterAddressUiState>() {
    private var address: Address? = null
    private var addressError: Throwable? = null
    private val canBeSendToAddress: Boolean
        get() = inputState is DataState.Success
    private var recentAddress: String? = "1EzZFZhopU4vBJKLiq5kUKphXZ4M22M1QjmEdfk"
    private val contacts = listOf(
        SContact("My Wallet", "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045"),
        SContact("TalgatETH", "0x95222290DD7278Aa3Ddd389Cc1E1d165CC4BAfe5"),
        SContact("Esso", "0x71C7656EC7ab88b098defB751B7401B5f6d8976F"),
        SContact("Escobar", "0x2B5AD5c4795c026514f8317c7a215E218DcCD6cF"),
        SContact("Vitalik", "0xAb5801a7D398351b8bE11C439e05C5B3259aeC9B"),
        SContact("Binance_1", "0x28C6c06298d514Db089934071355E5743bf21d60"),
        SContact("Kraken_Hot", "0x2910543Af39abA0Cd09dBb2D50200b3E800A63D2"),
        SContact("FTX_Exploiter", "0x59ABf3837Fa962d6853b4Cc0a19513AA031fd32b"),
        SContact("Coinbase_2", "0x503828976D22510aad0201ac7EC88293211D23Da"),
        SContact("Metamask_DEV", "0x9696f59E4d72E237BE84fFD425DCaD154Bf96976")
    )

    private var value = ""
    private var inputState: DataState<Address>? = null
    private var parseAddressJob: Job? = null

    override fun createState() = EnterAddressUiState(
        addressError = addressError,
        canBeSendToAddress = canBeSendToAddress,
        recentAddress = recentAddress,
        contacts = contacts,
        value = value,
        inputState = inputState
    )

    init {

    }

    fun onEnterAddress(value: String) {
        parseAddressJob?.cancel()

        if (value.isBlank()) {
            this.value = value
            address = null
            inputState = null
            emitState()
        } else {
            inputState = DataState.Loading
            emitState()

            parseForUri(value.trim())
        }
    }

    private fun parseForUri(text: String) {
        if (blockchainType == BlockchainType.Ton && text.contains("//")) {
            ToncoinUriParser.getAddress(text)?.let { address ->
                parseAddress(address)
                return
            }
        }
        when (val result = addressUriParser.parse(text)) {
            is AddressUriResult.Uri -> {
                parseAddress(result.addressUri.address)
            }

            AddressUriResult.InvalidBlockchainType -> {
                inputState = DataState.Error(AddressValidationException.Invalid(Throwable("Invalid Blockchain Type"), blockchainType.title))
                emitState()
            }

            AddressUriResult.InvalidTokenType -> {
                inputState = DataState.Error(AddressValidationException.Invalid(Throwable("Invalid Token Type"), blockchainType.title))
                emitState()
            }

            AddressUriResult.NoUri, AddressUriResult.WrongUri -> {
                parseAddress(text)
            }
        }
    }

    private fun parseAddress(addressText: String) {
        value = addressText
        emitState()

        parseAddressJob = viewModelScope.launch(Dispatchers.IO) {
            val handler = addressParserChain.supportedHandler(addressText)

            if (handler == null) {
                inputState = DataState.Error(AddressValidationException.Unsupported())
            } else {
                try {
                    val parsedAddress = handler.parseAddress(addressText)
                    address = parsedAddress
                    inputState = DataState.Success(parsedAddress)
                } catch (t: Throwable) {
                    inputState = DataState.Error(t)
                }
            }

            ensureActive()
            emitState()
        }
    }

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val blockchainType = wallet.token.blockchainType
            val coinCode = wallet.coin.code
            val tokenQuery = TokenQuery(blockchainType, wallet.token.type)
            val ensHandler = AddressHandlerEns(blockchainType, EnsResolverHolder.resolver)
            val udnHandler = AddressHandlerUdn(tokenQuery, coinCode, App.appConfigProvider.udnApiKey)
            val addressParserChain = AddressParserChain(domainHandlers = listOf(ensHandler, udnHandler))

            when (blockchainType) {
                BlockchainType.Bitcoin -> {
                    val network = MainNet()
                    addressParserChain.addHandler(AddressHandlerBase58(network, blockchainType))
                    addressParserChain.addHandler(AddressHandlerBech32(network, blockchainType))
                }

                BlockchainType.BitcoinCash -> {
                    val network = MainNetBitcoinCash()
                    addressParserChain.addHandler(AddressHandlerBase58(network, blockchainType))
                    addressParserChain.addHandler(AddressHandlerBitcoinCash(network, blockchainType))
                }

                BlockchainType.ECash -> {
                    val network = MainNetECash()
                    addressParserChain.addHandler(AddressHandlerBase58(network, blockchainType))
                    addressParserChain.addHandler(AddressHandlerBitcoinCash(network, blockchainType))
                }

                BlockchainType.Litecoin -> {
                    val network = MainNetLitecoin()
                    addressParserChain.addHandler(AddressHandlerBase58(network, blockchainType))
                    addressParserChain.addHandler(AddressHandlerBech32(network, blockchainType))
                }

                BlockchainType.Dash -> {
                    val network = MainNetDash()
                    addressParserChain.addHandler(AddressHandlerBase58(network, blockchainType))
                }

                BlockchainType.BinanceChain -> {
                    addressParserChain.addHandler(AddressHandlerBinanceChain())
                }

                BlockchainType.Zcash -> {
                    addressParserChain.addHandler(AddressHandlerZcash())
                }
                BlockchainType.Ethereum,
                BlockchainType.BinanceSmartChain,
                BlockchainType.Polygon,
                BlockchainType.Avalanche,
                BlockchainType.Optimism,
                BlockchainType.Base,
                BlockchainType.Gnosis,
                BlockchainType.Fantom,
                BlockchainType.ArbitrumOne -> {
                    addressParserChain.addHandler(AddressHandlerEvm(blockchainType))
                }
                BlockchainType.Solana -> {
                    addressParserChain.addHandler(AddressHandlerSolana())
                }
                BlockchainType.Tron -> {
                    addressParserChain.addHandler(AddressHandlerTron())
                }
                BlockchainType.Ton -> {
                    addressParserChain.addHandler(AddressHandlerTon())
                }
                is BlockchainType.Unsupported -> Unit
            }
            val addressUriParser = AddressUriParser(wallet.token.blockchainType, wallet.token.type)

            return EnterAddressViewModel(wallet.token.blockchainType, addressUriParser, addressParserChain) as T
        }
    }
}

data class EnterAddressUiState(
    val addressError: Throwable?,
    val canBeSendToAddress: Boolean,
    val recentAddress: String?,
    val contacts: List<SContact>,
    val value: String,
    val inputState: DataState<Address>?,
)
