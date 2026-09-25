package io.horizontalsystems.walletkit.modules.send.address

import io.horizontalsystems.marketkit.models.Token
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

        // Implicit accounts are created by the first transfer; a named account has to exist or
        // the transfer fails on chain. Null: the network could not answer, which must not reject
        // a well-formed id.
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
        if (exists == false) {
            throw AddressValidationError.InvalidAddress(Translator.getString(R.string.SwapSettings_Error_InvalidAddress))
        }
    }
}
