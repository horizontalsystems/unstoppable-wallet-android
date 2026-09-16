package io.horizontalsystems.walletkit.chain.zcash

import io.horizontalsystems.walletkit.core.adapters.zcash.ZcashAddressValidator
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.marketkit.models.BlockchainType
import timber.log.Timber

class AddressHandlerZcash : IAddressHandler {
    override val blockchainType = BlockchainType.Zcash

    override fun isSupported(value: String): Boolean {
        return ZcashAddressValidator.validate(value)
    }

    override fun parseAddress(value: String): Address {
        return Address(value, blockchainType = blockchainType)
    }

}

/**
 * Zcash Names (ZNS) input rules.
 *
 * On chain a name is 1 to 62 lowercase ASCII letters and digits. Users type it with a
 * `.zcash` or `.zec` suffix; the suffix is display-only and stripped before lookup.
 * The suffix is required here: a bare lowercase alphanumeric string is indistinguishable
 * from a partially typed address, and the send screen validates on every keystroke.
 */
object ZnsName {
    private val nameRegex = Regex("^[a-z0-9]{1,62}$")
    private val suffixes = listOf(".zcash", ".zec")

    /** Returns the on-chain name for [value], or null when it is not a ZNS name. */
    fun normalize(value: String): String? {
        val lowercased = value.trim().lowercase()
        val suffix = suffixes.firstOrNull { lowercased.endsWith(it) } ?: return null
        val name = lowercased.removeSuffix(suffix)
        return name.takeIf { nameRegex.matches(it) }
    }
}

class AddressHandlerZns(private val resolver: ZnsResolver) : IAddressHandler {
    override val blockchainType = BlockchainType.Zcash
    private val cache = mutableMapOf<String, Address>()

    override fun isSupported(value: String): Boolean {
        val name = ZnsName.normalize(value) ?: return false
        if (cache.containsKey(value)) return true

        return try {
            val resolved = resolver.resolve(name) ?: return false
            cache[value] = Address(resolved, value.trim(), blockchainType)
            true
        } catch (e: Exception) {
            Timber.w(e, "ZNS resolution failed for %s", name)
            false
        }
    }

    override fun parseAddress(value: String): Address {
        return cache[value]!!
    }
}
