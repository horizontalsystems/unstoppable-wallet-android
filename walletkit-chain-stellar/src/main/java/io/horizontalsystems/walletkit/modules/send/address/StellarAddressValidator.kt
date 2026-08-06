package io.horizontalsystems.walletkit.modules.send.address

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendStellarAdapter
import io.horizontalsystems.walletkit.core.adapters.NoTrustlineError
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.stellarkit.Network as StellarNetwork
import io.horizontalsystems.stellarkit.StellarKit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.horizontalsystems.stellarkit.room.StellarAsset

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
            } catch (_: NoTrustlineError) {
                false
            } catch (_: Exception) {
                // fatal Errors (OOM, LinkageError) are left to propagate
                null
            }
        }

        if (trustlineEstablished == false) {
            throw NoTrustlineError(tokenType.code)
        }
    }
}
