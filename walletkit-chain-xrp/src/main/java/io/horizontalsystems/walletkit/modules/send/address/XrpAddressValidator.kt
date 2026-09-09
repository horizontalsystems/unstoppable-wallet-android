package io.horizontalsystems.walletkit.modules.send.address

import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.xrpkit.XrpKit
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.adapters.NoTrustlineError
import io.horizontalsystems.walletkit.core.adapters.BaseXrpAdapter
import io.horizontalsystems.walletkit.core.managers.displayCode
import io.horizontalsystems.walletkit.entities.Address
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class XrpAddressValidator(private val token: Token) : EnterAddressValidator {
    private val adapter by lazy { App.adapterManager.getAdapterForToken<BaseXrpAdapter>(token) }

    override suspend fun validate(address: Address) {
        // format is decided locally and is authoritative
        XrpKit.validateAddress(address.hex)

        // native XRP needs no trust line
        val tokenType = token.type as? TokenType.XrpAsset ?: return

        // An issued token sent to an account without the trust line is rejected by the network,
        // so the recipient's trust line is part of address validity. Null means the state could
        // not be resolved (node unreachable): a well-formed address must not be rejected for that.
        val classic = XrpKit.decodeXAddress(address.hex)?.first ?: address.hex
        val trustLineSet: Boolean? = withContext(Dispatchers.IO) {
            try {
                adapter?.kit?.isTrustLineSet(tokenType.currency, tokenType.issuer, classic)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }

        if (trustLineSet == false) {
            throw NoTrustlineError(tokenType.displayCode)
        }
    }
}
