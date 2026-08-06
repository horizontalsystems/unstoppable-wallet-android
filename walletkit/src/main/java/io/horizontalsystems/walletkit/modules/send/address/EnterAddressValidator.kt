package io.horizontalsystems.walletkit.modules.send.address

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.ISendBitcoinAdapter
import io.horizontalsystems.walletkit.core.ISendStellarAdapter
import io.horizontalsystems.walletkit.core.ISendThorchainAdapter
import io.horizontalsystems.walletkit.core.ISendTronAdapter
import io.horizontalsystems.walletkit.core.adapters.StellarAssetAdapter
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoincore.utils.AddressConverterChain
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.CashAddressConverter
import io.horizontalsystems.bitcoincore.utils.SegwitAddressConverter
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.dashkit.MainNetDash
import io.horizontalsystems.ecash.MainNetECash
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.litecoinkit.MainNetLitecoin
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.stellarkit.Network as StellarNetwork
import io.horizontalsystems.stellarkit.StellarKit
import io.horizontalsystems.stellarkit.room.StellarAsset
import io.horizontalsystems.walletkit.core.managers.thorchainNetwork
import io.horizontalsystems.tonkit.FriendlyAddress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

interface EnterAddressValidator {
    @Throws
    suspend fun validate(address: Address)
}

class BitcoinAddressValidator(
    private val token: Token,
    private val adapterManager: IAdapterManager
) : EnterAddressValidator {
    private val sendAdapter by lazy { adapterManager.getAdapterForToken<ISendBitcoinAdapter>(token) }

    override suspend fun validate(address: Address) {
        val adapter = sendAdapter
        if (adapter != null) {
            adapter.validate(address.hex, null)
        } else {
            // no enabled wallet for this chain (e.g. a swap recipient on an account that
            // can't hold it) — parse with the chain's mainnet address converters instead
            staticAddressConverter(token.blockchainType).convert(address.hex)
        }
    }

    companion object {
        // Mirrors each kit's private parseAddress(): the same converters BitcoinCore
        // builds for the network, minus the wallet
        private fun staticAddressConverter(blockchainType: BlockchainType) = AddressConverterChain().apply {
            when (blockchainType) {
                BlockchainType.Bitcoin -> {
                    val network = MainNet()
                    prependConverter(SegwitAddressConverter(network.addressSegwitHrp))
                    prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))
                }

                BlockchainType.Litecoin -> {
                    val network = MainNetLitecoin()
                    prependConverter(SegwitAddressConverter(network.addressSegwitHrp))
                    prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))
                }

                BlockchainType.BitcoinCash -> {
                    val network = MainNetBitcoinCash()
                    prependConverter(CashAddressConverter(network.addressSegwitHrp))
                    prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))
                }

                BlockchainType.ECash -> {
                    val network = MainNetECash()
                    prependConverter(CashAddressConverter(network.addressSegwitHrp))
                    prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))
                }

                BlockchainType.Dash -> {
                    val network = MainNetDash()
                    prependConverter(Base58AddressConverter(network.addressVersion, network.addressScriptVersion))
                }

                else -> throw AddressValidationError.NoAdapter()
            }
        }
    }
}

class EvmAddressValidator : EnterAddressValidator {
    override suspend fun validate(address: Address) {
        AddressValidator.validate(address.hex)
        io.horizontalsystems.ethereumkit.models.Address(address.hex)
    }
}

class SolanaAddressValidator : EnterAddressValidator {
    override suspend fun validate(address: Address) {
        io.horizontalsystems.solanakit.models.Address(address.hex)
    }
}

class TonAddressValidator : EnterAddressValidator {
    override suspend fun validate(address: Address) {
        FriendlyAddress.parse(address.hex)
    }
}

class StellarAddressValidator(private val token: Token) : EnterAddressValidator {
    private val sendAdapter by lazy { App.adapterManager.getAdapterForToken<ISendStellarAdapter>(token) }

    override suspend fun validate(address: Address) {
        // format is decided locally and is authoritative
        StellarKit.validateAddress(address.hex)

        // native XLM needs no trustline
        val tokenType = token.type as? TokenType.Asset ?: return

        // A non-native asset sent to an account without the trustline is rejected by the
        // network, so the recipient's trustline is part of address validity. Null means
        // the state could not be resolved (Horizon unreachable) — a well-formed address
        // must not be rejected for that; the network stays the authority at send time.
        val trustlineEstablished = withContext(Dispatchers.IO) {
            try {
                sendAdapter?.let { adapter ->
                    adapter.validate(address.hex)
                    true
                } ?: StellarKit.isAssetEnabled(
                    StellarNetwork.MainNet,
                    StellarAsset.Asset(tokenType.code, tokenType.issuer),
                    address.hex
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: StellarAssetAdapter.NoTrustlineError) {
                false
            } catch (_: Exception) {
                // fatal Errors (OOM, LinkageError) are left to propagate
                null
            }
        }

        if (trustlineEstablished == false) {
            throw StellarAssetAdapter.NoTrustlineError(tokenType.code)
        }
    }
}

class ThorchainAddressValidator(private val token: Token) : EnterAddressValidator {
    private val sendAdapter by lazy { App.adapterManager.getAdapterForToken<ISendThorchainAdapter>(token) }
    override suspend fun validate(address: Address) {
        val adapter = sendAdapter
        if (adapter != null) {
            adapter.validate(address.hex)
        } else {
            // no enabled wallet (external swap recipient) — static parse against the chain's network
            io.horizontalsystems.thorchainkit.models.Address.fromString(address.hex, token.blockchainType.thorchainNetwork())
        }
    }
}


class TronAddressValidator(
    private val token: Token,
    private val adapterManager: IAdapterManager,
    private val allowOwnAddress: Boolean = false,
) : EnterAddressValidator {
    private val sendAdapter by lazy { adapterManager.getAdapterForToken<ISendTronAdapter>(token) }
    override suspend fun validate(address: Address) {
        val validAddress = io.horizontalsystems.tronkit.models.Address.fromBase58(address.hex)

        if (allowOwnAddress) return

        // adapter may be absent (external swap recipient) — then there is no own
        // address to protect against and the format check above suffices
        val adapter = sendAdapter
        if (adapter != null && token.type == TokenType.Native && adapter.isOwnAddress(validAddress)) {
            throw AddressValidationError.SendToSelfForbidden(
                Translator.getString(R.string.Send_Error_SendToSelf, "TRX")
            )
        }
    }
}



sealed class AddressValidationError : Throwable() {
    class NoAdapter : AddressValidationError() {
        override val message = "Send adapter is not found"
    }
    class SendToSelfForbidden(override val message: String) : AddressValidationError()
    class InvalidAddress(override val message: String) : AddressValidationError()
}