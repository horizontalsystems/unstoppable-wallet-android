package cash.p.terminal.modules.address

import cash.p.terminal.core.utils.AddressUriParser
import cash.p.terminal.core.utils.AddressUriResult
import cash.p.terminal.core.utils.ToncoinUriParser
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.DataState
import cash.p.terminal.wallet.title
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class FullAddressParserUseCase(
    val blockchainType: BlockchainType,
    private val addressUriParser: AddressUriParser,
    private val addressParserChain: AddressParserChain,
) {

    private val _valueFlow = MutableStateFlow<String>("")
    val valueFlow = _valueFlow.asStateFlow()

    private val _inputState = MutableStateFlow<DataState<Address>?>(null)
    val inputState = _inputState.asStateFlow()

    private val _address = MutableStateFlow<Address?>(null)
    val address = _address.asStateFlow()

    private var parseAddressJob: Job? = null

    suspend fun parseText(value: String) {
        parseAddressJob?.cancel()

        if (value.isBlank()) {
            _valueFlow.value = value
            _inputState.value = null
            _address.value = null
            return
        }

        val vTrimmed = value.trim()

        _inputState.value = DataState.Loading

        parseForUri(vTrimmed)
    }

    private suspend fun parseForUri(text: String) {
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
                _inputState.value = DataState.Error(
                    AddressValidationException.Invalid(
                        Throwable("Invalid Blockchain Type"),
                        blockchainType.title
                    )
                )
            }

            AddressUriResult.InvalidTokenType -> {
                _inputState.value = DataState.Error(
                    AddressValidationException.Invalid(
                        Throwable("Invalid Token Type"),
                        blockchainType.title
                    )
                )
            }

            AddressUriResult.NoUri, AddressUriResult.WrongUri -> {
                parseAddress(text)
            }
        }
    }

    suspend fun parseAddress(addressText: String) = withContext(Dispatchers.Default) {
        _valueFlow.value = addressText
        val handler = addressParserChain.supportedHandler(addressText) ?: run {
            ensureActive()
            withContext(Dispatchers.Main) {
                _inputState.value = DataState.Error(AddressValidationException.Unsupported())
            }
            return@withContext
        }

        try {
            val parsedAddress = handler.parseAddress(addressText)
            ensureActive()
            withContext(Dispatchers.Main) {
                _address.value = parsedAddress
                _inputState.value = DataState.Success(parsedAddress)
            }
        } catch (t: Throwable) {
            ensureActive()
            withContext(Dispatchers.Main) {
                _inputState.value = DataState.Error(t)
            }
        }
    }

    fun onAddressError(addressError: Throwable?) {
        addressError?.let {
            _inputState.value = DataState.Error(addressError)
        }
    }
}