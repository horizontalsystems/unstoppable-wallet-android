package io.horizontalsystems.bankwallet.modules.send.address

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.core.ISendTronAdapter
import io.horizontalsystems.bankwallet.core.ISendZcashAdapter
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter.ZcashError
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tonkit.FriendlyAddress

interface EnterAddressValidator {
    @Throws
    suspend fun validate(address: Address)
}

class BitcoinAddressValidator(
    private val adapter: ISendBitcoinAdapter
) : EnterAddressValidator {
    override suspend fun validate(address: Address) {
        adapter.validate(address.hex, null)
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

class TronAddressValidator(
    private val adapter: ISendTronAdapter,
    private val token: Token
) : EnterAddressValidator {
    override suspend fun validate(address: Address) {
        val validAddress = io.horizontalsystems.tronkit.models.Address.fromBase58(address.hex)
        if (token.type == TokenType.Native && adapter.isOwnAddress(validAddress)) {
            throw AddressValidationError.SendToSelfForbidden(
                Translator.getString(R.string.Send_Error_SendToSelf, "TRX")
            )
        }
    }
}

class ZcashAddressValidator(
    private val adapter: ISendZcashAdapter
) : EnterAddressValidator {
    override suspend fun validate(address: Address) {
        try {
            adapter.validate(address.hex)
        } catch (e: ZcashError.SendToSelfNotAllowed) {
            throw AddressValidationError.SendToSelfForbidden(
                Translator.getString(R.string.Send_Error_SendToSelf, "ZEC")
            )
        }
    }
}

sealed class AddressValidationError : Throwable() {
    class SendToSelfForbidden(override val message: String) : AddressValidationError()
}