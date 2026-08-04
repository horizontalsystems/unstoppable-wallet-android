package io.horizontalsystems.walletkit.modules.send.address

import cash.z.ecc.android.sdk.internal.jni.RustBackend
import cash.z.ecc.android.sdk.model.ZcashNetwork
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.ISendBitcoinAdapter
import io.horizontalsystems.walletkit.core.ISendStellarAdapter
import io.horizontalsystems.walletkit.core.ISendThorchainAdapter
import io.horizontalsystems.walletkit.core.ISendTronAdapter
import io.horizontalsystems.walletkit.core.ISendZcashAdapter
import io.horizontalsystems.walletkit.core.adapters.zcash.ZcashAdapter.ZcashError
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
import io.horizontalsystems.thorchainkit.network.Network
import io.horizontalsystems.tonkit.FriendlyAddress
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
        val adapter = sendAdapter
        if (adapter != null) {
            adapter.validate(address.hex)
            return
        }

        // no enabled wallet (external swap recipient) — static format check, plus the
        // same trustline requirement the adapter enforces: a non-native asset sent to
        // an account without the trustline is rejected by the network
        StellarKit.validateAddress(address.hex)

        val tokenType = token.type
        if (tokenType is TokenType.Asset) {
            val enabled = withContext(Dispatchers.IO) {
                StellarKit.isAssetEnabled(
                    StellarNetwork.MainNet,
                    StellarAsset.Asset(tokenType.code, tokenType.issuer),
                    address.hex
                )
            }
            if (!enabled) {
                throw AddressValidationError.InvalidAddress(
                    Translator.getString(R.string.Swap_RecipientAddress_NoTrustline, tokenType.code)
                )
            }
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
            // no enabled wallet (external swap recipient) — static mainnet parse
            io.horizontalsystems.thorchainkit.models.Address.fromString(address.hex, Network.Mainnet)
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

class ZcashAddressValidator(
    private val token: Token,
    private val adapterManager: IAdapterManager,
    private val allowOwnAddress: Boolean = false,
    // the selected swap route delivers only to transparent addresses (CEX providers);
    // shielded/unified recipients would be rejected at order creation
    private val transparentOnly: Boolean = false,
) : EnterAddressValidator {
    private val sendAdapter by lazy { adapterManager.getAdapterForToken<ISendZcashAdapter>(token) }

    override suspend fun validate(address: Address) {
        val adapter = sendAdapter
        if (adapter == null) {
            // no enabled ZEC wallet (external swap recipient) — validate with the rust
            // backend directly instead of the wallet's synchronizer
            if (!ZcashStaticAddressValidator.isValid(address.hex)) {
                throw AddressValidationError.InvalidAddress(
                    Translator.getString(R.string.Send_Address_Error_InvalidAddress)
                )
            }
        } else {
            try {
                adapter.validate(address.hex)
            } catch (e: ZcashError.SendToSelfNotAllowed) {
                // the adapter recognized the wallet's own (valid) address — a legitimate
                // recipient when allowed (swap delivery target)
                if (!allowOwnAddress) {
                    throw AddressValidationError.SendToSelfForbidden(
                        Translator.getString(R.string.Send_Error_SendToSelf, "ZEC")
                    )
                }
            }
        }

        if (transparentOnly && !ZcashStaticAddressValidator.isTransparent(address.hex)) {
            throw AddressValidationError.InvalidAddress(
                Translator.getString(R.string.Swap_RecipientAddress_TransparentZcashRequired)
            )
        }
    }
}

// Address validation via the SDK's rust backend without a synchronizer. The backend's
// address checks only forward (address, networkId) to JNI — the db/params paths are
// stored but never read — so a backend over dummy paths is safe and cheap. Memoized:
// RustBackend.new loads the native library once.
private object ZcashStaticAddressValidator {
    private val mutex = Mutex()
    private var backend: RustBackend? = null

    private suspend fun backend(): RustBackend = mutex.withLock {
        backend ?: run {
            val dummyDir = File(App.instance.cacheDir, "zcash-address-check")
            RustBackend.new(
                fsBlockDbRoot = File(dummyDir, "fs"),
                dataDbFile = File(dummyDir, "data.db"),
                saplingSpendFile = File(dummyDir, "spend.params"),
                saplingOutputFile = File(dummyDir, "output.params"),
                zcashNetworkId = ZcashNetwork.Mainnet.id,
            ).also { backend = it }
        }
    }

    suspend fun isValid(address: String): Boolean = withContext(Dispatchers.IO) {
        val backend = backend()
        backend.isValidTransparentAddr(address) ||
            backend.isValidSaplingAddr(address) ||
            backend.isValidUnifiedAddr(address) ||
            backend.isValidTexAddr(address)
    }

    // TEX counts as transparent, mirroring ZcashAdapter's AddressType.Tex mapping
    suspend fun isTransparent(address: String): Boolean = withContext(Dispatchers.IO) {
        val backend = backend()
        backend.isValidTransparentAddr(address) || backend.isValidTexAddr(address)
    }
}

sealed class AddressValidationError : Throwable() {
    class NoAdapter : AddressValidationError() {
        override val message = "Send adapter is not found"
    }
    class SendToSelfForbidden(override val message: String) : AddressValidationError()
    class InvalidAddress(override val message: String) : AddressValidationError()
}