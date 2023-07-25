package cash.p.terminal.modules.withdrawcex

import cash.p.terminal.entities.Address
import cash.p.terminal.entities.DataState
import cash.p.terminal.modules.address.AddressHandlerBase58
import cash.p.terminal.modules.address.AddressHandlerBech32
import cash.p.terminal.modules.address.AddressHandlerBinanceChain
import cash.p.terminal.modules.address.AddressHandlerBitcoinCash
import cash.p.terminal.modules.address.AddressHandlerEns
import cash.p.terminal.modules.address.AddressHandlerEvm
import cash.p.terminal.modules.address.AddressHandlerPure
import cash.p.terminal.modules.address.AddressHandlerSolana
import cash.p.terminal.modules.address.AddressHandlerTron
import cash.p.terminal.modules.address.AddressHandlerUdn
import cash.p.terminal.modules.address.EnsResolverHolder
import cash.p.terminal.modules.address.IAddressHandler
import cash.p.terminal.modules.contacts.ContactAddressParser
import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.dashkit.MainNetDash
import io.horizontalsystems.ecash.MainNetECash
import io.horizontalsystems.litecoinkit.MainNetLitecoin
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CexWithdrawAddressService {

    private var address: String? = null
    private var addressParser: ContactAddressParser? = null
    private var state: DataState<Address>? = null
        set(value) {
            field = value
            _stateFlow.update { value }
        }

    private val _stateFlow = MutableStateFlow<DataState<Address>?>(null)
    val stateFlow = _stateFlow.asStateFlow()

    suspend fun setAddress(address: String?) {
        this.address = address

        validateAddress()
    }

    suspend fun setBlockchain(blockchain: Blockchain?) {
        this.addressParser = blockchain?.let { addressParser(it) }

        validateAddress()
    }

    private var validationJob: Job? = null

    private suspend fun validateAddress() = withContext(Dispatchers.IO) {
        validationJob?.cancel()

        val address = address
        if (address.isNullOrEmpty()) {
            state = null
            return@withContext
        }

        validationJob = launch {
            state = DataState.Loading

            state = try {
                val parsedAddress = addressParser?.parseAddress(address) ?: Address(address)
                ensureActive()
                DataState.Success(parsedAddress)
            } catch (error: Throwable) {
                ensureActive()
                DataState.Error(error)
            }
        }
    }

    private fun addressParser(blockchain: Blockchain): ContactAddressParser {
        val udnHandler = AddressHandlerUdn(TokenQuery(blockchain.type, TokenType.Native), "")
        val domainAddressHandlers = mutableListOf<IAddressHandler>(udnHandler)
        val rawAddressHandlers = mutableListOf<IAddressHandler>()

        when (blockchain.type) {
            BlockchainType.Bitcoin -> {
                val network = MainNet()
                rawAddressHandlers.add(AddressHandlerBase58(network))
                rawAddressHandlers.add(AddressHandlerBech32(network))
            }

            BlockchainType.BitcoinCash -> {
                val network = MainNetBitcoinCash()
                rawAddressHandlers.add(AddressHandlerBase58(network))
                rawAddressHandlers.add(AddressHandlerBitcoinCash(network))
            }

            BlockchainType.ECash -> {
                val network = MainNetECash()
                rawAddressHandlers.add(AddressHandlerBase58(network))
                rawAddressHandlers.add(AddressHandlerBitcoinCash(network))
            }

            BlockchainType.Litecoin -> {
                val network = MainNetLitecoin()
                rawAddressHandlers.add(AddressHandlerBase58(network))
                rawAddressHandlers.add(AddressHandlerBech32(network))
            }

            BlockchainType.Dash -> {
                val network = MainNetDash()
                rawAddressHandlers.add(AddressHandlerBase58(network))
            }

            BlockchainType.BinanceChain -> {
                rawAddressHandlers.add(AddressHandlerBinanceChain())
            }

            BlockchainType.Zcash -> {
                //No validation
                rawAddressHandlers.add(AddressHandlerPure())
            }

            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
            BlockchainType.ArbitrumOne -> {
                domainAddressHandlers.add(AddressHandlerEns(EnsResolverHolder.resolver))
                rawAddressHandlers.add(AddressHandlerEvm())
            }

            BlockchainType.Solana -> {
                rawAddressHandlers.add(AddressHandlerSolana())
            }

            BlockchainType.Tron -> {
                rawAddressHandlers.add(AddressHandlerTron())
            }

            is BlockchainType.Unsupported -> {
            }
        }

        return ContactAddressParser(domainAddressHandlers, rawAddressHandlers, blockchain)
    }
}
