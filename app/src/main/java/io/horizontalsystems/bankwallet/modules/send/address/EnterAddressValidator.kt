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
    suspend fun validate(address: Address): AddressCheckResult
}

class BitcoinAddressValidator(
    private val adapter: ISendBitcoinAdapter
) : EnterAddressValidator {
    override suspend fun validate(address: Address): AddressCheckResult {
        try {
            adapter.validate(address.hex, null)
        } catch (e: Throwable) {
            return AddressCheckResult.Incorrect()
        }
        return AddressCheckResult.Correct
    }
}

class EvmAddressValidator : EnterAddressValidator {
    override suspend fun validate(address: Address): AddressCheckResult {
        try {
            AddressValidator.validate(address.hex)
            io.horizontalsystems.ethereumkit.models.Address(address.hex)
        } catch (e: Throwable) {
            return AddressCheckResult.Incorrect()
        }
        return AddressCheckResult.Correct
    }
}

class SolanaAddressValidator : EnterAddressValidator {
    override suspend fun validate(address: Address): AddressCheckResult {
        try {
            io.horizontalsystems.solanakit.models.Address(address.hex)
        } catch (e: Throwable) {
            return AddressCheckResult.Incorrect()
        }
        return AddressCheckResult.Correct
    }
}

class TonAddressValidator : EnterAddressValidator {
    override suspend fun validate(address: Address): AddressCheckResult {
        try {
            FriendlyAddress.parse(address.hex)
        } catch (e: Throwable) {
            return AddressCheckResult.Incorrect()
        }
        return AddressCheckResult.Correct
    }
}

class TronAddressValidator(
    private val adapter: ISendTronAdapter,
    private val token: Token
) : EnterAddressValidator {
    override suspend fun validate(address: Address): AddressCheckResult {
        try {
            val validAddress = io.horizontalsystems.tronkit.models.Address.fromBase58(address.hex)
            if (token.type == TokenType.Native && adapter.isOwnAddress(validAddress)) {
                return AddressCheckResult.Incorrect(Translator.getString(R.string.Send_Error_SendToSelf, "TRX"))
            }
        } catch (e: Throwable) {
            return AddressCheckResult.Incorrect()
        }
        return AddressCheckResult.Correct
    }
}

class ZcashAddressValidator(
    private val adapter: ISendZcashAdapter
) : EnterAddressValidator {
    override suspend fun validate(address: Address): AddressCheckResult {
        try {
            adapter.validate(address.hex)
        } catch (e: ZcashError.SendToSelfNotAllowed) {
            return AddressCheckResult.Incorrect(Translator.getString(R.string.Send_Error_SendToSelf, "ZEC"))
        } catch (e: Throwable) {
            return AddressCheckResult.Incorrect()
        }
        return AddressCheckResult.Correct
    }
}
