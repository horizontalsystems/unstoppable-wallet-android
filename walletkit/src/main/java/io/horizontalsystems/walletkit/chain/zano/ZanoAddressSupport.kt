package io.horizontalsystems.walletkit.chain.zano

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.send.address.AddressValidationError
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.zanokit.ZanoKit

class AddressHandlerZano : IAddressHandler {
    override val blockchainType = BlockchainType.Zano

    override fun isSupported(value: String) = try {
        ZanoKit.isValidAddress(value)
    } catch (_: Exception) {
        false
    }

    override fun parseAddress(value: String): Address {
        return Address(value, blockchainType = blockchainType)
    }
}

class AddressHandlerZanoAlias(private val resolver: ZanoAliasResolver) : IAddressHandler {
    override val blockchainType = BlockchainType.Zano
    private val cache = mutableMapOf<String, Address>()

    override fun isSupported(value: String): Boolean {
        val alias = normalize(value) ?: return false
        if (cache.containsKey(value)) return true

        return try {
            val resolved = resolver.resolve(alias) ?: return false
            cache[value] = Address(resolved, value, blockchainType)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun parseAddress(value: String): Address {
        return cache[value]!!
    }

    companion object {
        // on-chain alias charset is a-z 0-9 . - with max length 25
        private val aliasRegex = Regex("^[a-z0-9.-]{1,25}\$")

        fun normalize(value: String): String? {
            val hasPrefix = value.startsWith("@")
            val alias = if (hasPrefix) value.substring(1) else value
            if (!aliasRegex.matches(alias)) return null
            // without "@" the intent is ambiguous, require 2+ chars to avoid a lookup on the first keystroke
            if (!hasPrefix && alias.length < 2) return null
            return alias
        }
    }
}

class ZanoAddressValidator : EnterAddressValidator {
    override suspend fun validate(address: Address) {
        if (!ZanoKit.isValidAddress(address.hex)) {
            throw AddressValidationError.InvalidAddress(
                Translator.getString(R.string.Send_Address_Error_InvalidAddress)
            )
        }
    }
}
