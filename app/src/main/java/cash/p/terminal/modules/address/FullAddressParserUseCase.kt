package cash.p.terminal.modules.address

import cash.p.terminal.core.utils.AddressUriParser
import cash.p.terminal.core.utils.AddressUriResult
import cash.p.terminal.core.utils.ToncoinUriParser
import cash.p.terminal.entities.Address
import cash.p.terminal.ui_compose.entities.DataState
import cash.p.terminal.wallet.title
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FullAddressParserUseCase(
    val blockchainType: BlockchainType,
    private val addressUriParser: AddressUriParser,
    private val addressParserChain: AddressParserChain,
    private val coroutineScope: CoroutineScope,
) {

    private val _valueFlow = MutableStateFlow("")
    val valueFlow = _valueFlow.asStateFlow()

    private val _inputState = MutableStateFlow<DataState<Address>?>(null)
    val inputState = _inputState.asStateFlow()

    private val _address = MutableStateFlow<Address?>(null)
    val address = _address.asStateFlow()

    private var parseJob: Job? = null

    /**
     * Called when the user types or edits text in the TextField.
     * Updates [_valueFlow] immediately so the TextField stays in sync,
     * then launches async validation without overwriting [_valueFlow] again.
     */
    fun parseText(value: String) {
        launchValidation(value) { validateForUri(value.trim()) }
    }

    /**
     * Called programmatically (initial address, QR scan) — not from user typing.
     * Updates [_valueFlow] to display the address in the TextField.
     */
    fun parseAddress(addressText: String) {
        launchValidation(addressText) { validateAddress(addressText) }
    }

    private fun launchValidation(value: String, block: suspend () -> Unit) {
        parseJob?.cancel()
        _valueFlow.value = value

        if (value.isBlank()) {
            _inputState.value = null
            _address.value = null
            return
        }

        _inputState.value = DataState.Loading
        parseJob = coroutineScope.launch { block() }
    }

    fun onAddressError(addressError: Throwable?) {
        addressError?.let {
            _inputState.value = DataState.Error(addressError)
        }
    }

    /**
     * Parses URI format, then validates the extracted address.
     * Only updates [_valueFlow] when extracting address from URI format.
     */
    private suspend fun validateForUri(text: String) {
        coroutineContext.ensureActive()
        if (blockchainType == BlockchainType.Ton && text.contains("//")) {
            ToncoinUriParser.getAddress(text)?.let { tonAddress ->
                coroutineContext.ensureActive()
                _valueFlow.value = tonAddress
                validateAddress(tonAddress)
                return
            }
        }
        when (val result = addressUriParser.parse(text)) {
            is AddressUriResult.Uri -> {
                coroutineContext.ensureActive()
                _valueFlow.value = result.addressUri.address
                validateAddress(result.addressUri.address)
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
                validateAddress(text)
            }
        }
    }

    /**
     * Validates an address on a background thread.
     * Only updates [_inputState] and [_address] — never [_valueFlow].
     */
    private suspend fun validateAddress(addressText: String) = withContext(Dispatchers.Default) {
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
}
