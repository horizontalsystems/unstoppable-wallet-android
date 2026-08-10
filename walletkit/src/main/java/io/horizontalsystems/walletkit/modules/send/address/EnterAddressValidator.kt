package io.horizontalsystems.walletkit.modules.send.address

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.ISendTronAdapter
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.core.address.EvmAddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
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


class EvmAddressValidator : EnterAddressValidator {
    override suspend fun validate(address: Address) {
        io.horizontalsystems.walletkit.core.address.EvmAddressValidator.validate(address.hex)
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