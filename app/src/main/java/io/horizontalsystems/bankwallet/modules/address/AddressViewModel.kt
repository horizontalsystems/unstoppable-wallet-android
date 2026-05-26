package io.horizontalsystems.bankwallet.modules.address

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.title
import io.horizontalsystems.bankwallet.core.utils.AddressUriParser
import io.horizontalsystems.bankwallet.core.utils.AddressUriResult
import io.horizontalsystems.bankwallet.core.utils.ToncoinUriParser
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
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
import kotlinx.coroutines.withContext

@HiltViewModel(assistedFactory = AddressViewModel.Factory::class)
class AddressViewModel @AssistedInject constructor(
    @Assisted private val tokenQuery: TokenQuery,
    @Assisted private val coinCode: String,
    @Assisted initial: Address?,
    appConfigProvider: AppConfigProvider,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(tokenQuery: TokenQuery, coinCode: String, initial: Address?): AddressViewModel
    }

    val blockchainType: BlockchainType = tokenQuery.blockchainType
    private val addressUriParser: AddressUriParser
    private val addressParserChain: AddressParserChain

    init {
        val ensHandler = AddressHandlerEns(blockchainType, EnsResolverHolder.resolver)
        val udnHandler = AddressHandlerUdn(tokenQuery, coinCode, appConfigProvider.udnApiKey)
        addressParserChain = AddressParserChain(domainHandlers = listOf(ensHandler, udnHandler))

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

            BlockchainType.Zcash -> {
                addressParserChain.addHandler(AddressHandlerZcash())
            }
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.ECash,
            BlockchainType.Litecoin,
            BlockchainType.Dash,
            BlockchainType.Zcash -> {
                addressParserChain.addHandler(AddressHandlerPure(blockchainType))
            }
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.Base,
            BlockchainType.ZkSync,
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
            BlockchainType.Stellar -> {
                addressParserChain.addHandler(AddressHandlerStellar())
            }
            BlockchainType.Monero -> {
                addressParserChain.addHandler(AddressHandlerMonero())
            }
            BlockchainType.Zano -> {
                addressParserChain.addHandler(AddressHandlerZano())
            }
            is BlockchainType.Unsupported -> Unit
        }

        addressUriParser = AddressUriParser(blockchainType, tokenQuery.tokenType)
    }

    var address by mutableStateOf<Address?>(initial)
        private set
    var inputState by mutableStateOf<DataState<Address>?>(null)
        private set
    var value by mutableStateOf(initial?.hex ?: "")
        private set
    private var parseAddressJob: Job? = null

    init {
        initial?.hex?.let {
            parseAddress(it)
        }
    }

    fun parseText(value: String) {
        parseAddressJob?.cancel()

        if (value.isBlank()) {
            this.value = value
            inputState = null
            address = null
            return
        }

        val vTrimmed = value.trim()

        inputState = DataState.Loading

        parseForUri(vTrimmed)
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
            }

            AddressUriResult.InvalidTokenType -> {
                inputState = DataState.Error(AddressValidationException.Invalid(Throwable("Invalid Token Type"), blockchainType.title))
            }

            AddressUriResult.NoUri, AddressUriResult.WrongUri -> {
                parseAddress(text)
            }
        }
    }

    private fun parseAddress(addressText: String) {
        value = addressText
        parseAddressJob = viewModelScope.launch(Dispatchers.IO) {
            val handler = addressParserChain.supportedHandler(addressText) ?: run {
                ensureActive()
                withContext(Dispatchers.Main) {
                    inputState = DataState.Error(AddressValidationException.Unsupported())
                }
                return@launch
            }

            try {
                val parsedAddress = handler.parseAddress(addressText)
                ensureActive()
                withContext(Dispatchers.Main) {
                    address = parsedAddress
                    inputState = DataState.Success(parsedAddress)
                }
            } catch (t: Throwable) {
                ensureActive()
                withContext(Dispatchers.Main) {
                    inputState = DataState.Error(t)
                }
            }
        }
    }

    fun onAddressError(addressError: Throwable?) {
        addressError?.let {
            inputState = DataState.Error(addressError)
        }
    }
}
