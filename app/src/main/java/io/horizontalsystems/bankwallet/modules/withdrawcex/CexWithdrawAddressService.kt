package io.horizontalsystems.bankwallet.modules.withdrawcex

import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerBase58
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerBech32
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerBinanceChain
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerBitcoinCash
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerEns
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerEvm
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerPure
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerSolana
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerTron
import io.horizontalsystems.bankwallet.modules.address.AddressHandlerUdn
import io.horizontalsystems.bankwallet.modules.address.EnsResolverHolder
import io.horizontalsystems.bankwallet.modules.address.IAddressHandler
import io.horizontalsystems.bankwallet.modules.contacts.ContactAddressParser
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

class CexWithdrawAddressService(
    blockchain: Blockchain?
) {
    private var address: String? = null
    private var addressParser: ContactAddressParser? = blockchain?.let { addressParser(blockchain) }
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
