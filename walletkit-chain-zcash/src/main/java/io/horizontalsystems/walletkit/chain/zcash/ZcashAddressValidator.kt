package io.horizontalsystems.walletkit.chain.zcash

import cash.z.ecc.android.sdk.internal.jni.RustBackend
import cash.z.ecc.android.sdk.model.ZcashNetwork
import io.horizontalsystems.walletkit.modules.send.address.AddressValidationError
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.walletkit.core.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.ISendZcashAdapter
import io.horizontalsystems.walletkit.core.adapters.zcash.ZcashAdapter.ZcashError
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.marketkit.models.Token

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
internal object ZcashStaticAddressValidator {
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
