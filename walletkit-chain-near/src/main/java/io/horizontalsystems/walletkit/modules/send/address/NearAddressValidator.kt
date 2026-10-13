package io.horizontalsystems.walletkit.modules.send.address

import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.nearkit.NearKit
import io.horizontalsystems.nearkit.crypto.AccountId
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.adapters.BaseNearAdapter
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.Address
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NearAddressValidator(
    private val token: Token,
    private val allowOwnAddress: Boolean = false,
) : EnterAddressValidator {
    private val adapter by lazy { App.adapterManager.getAdapterForToken<BaseNearAdapter>(token) }

    override suspend fun validate(address: Address) {
        NearKit.validateAccountId(address.hex)

        if (!allowOwnAddress && adapter?.receiveAddress == address.hex) {
            throw AddressValidationError.SendToSelfForbidden(
                Translator.getString(R.string.Send_Error_SendToSelf, token.coin.code)
            )
        }

        // a token contract credits tokens sent to itself, and only the issuer can return them
        if ((token.type as? TokenType.Nep141)?.contractId == address.hex) {
            throw AddressValidationError.InvalidAddress(Translator.getString(R.string.NearSend_ReceiverIsTokenContract))
        }

        // Implicit accounts are created by the first transfer. A named account has to exist: a
        // NEAR transfer to a missing one fails on chain, but a token contract credits any id, and
        // tokens sent to an id nobody owns are lost. So an unanswered check blocks too.
        if (AccountId.isImplicit(address.hex)) return
        val exists: Boolean? = withContext(Dispatchers.IO) {
            try {
                adapter?.kit?.doesAccountExist(address.hex)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
        when (exists) {
            true -> Unit
            false -> throw AddressValidationError.InvalidAddress(Translator.getString(R.string.SwapSettings_Error_InvalidAddress))
            null -> throw AddressValidationError.InvalidAddress(Translator.getString(R.string.NearSend_ReceiverCheckFailed))
        }
    }
}
