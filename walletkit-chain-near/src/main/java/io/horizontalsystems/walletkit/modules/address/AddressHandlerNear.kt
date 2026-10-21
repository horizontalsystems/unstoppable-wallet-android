package io.horizontalsystems.walletkit.modules.address

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.nearkit.NearKit
import io.horizontalsystems.nearkit.crypto.AccountId
import io.horizontalsystems.walletkit.entities.Address

/**
 * Recognizes what can only be a NEAR account: implicit (64 hex), ETH-implicit (`0x` + 40 hex) and
 * `.near` names. Other dotted names (`alice.tg`) are valid NEAR ids too, but may belong to ENS,
 * Unstoppable Domains or ZNS, so parsers that try several chains do not take them for NEAR.
 */
class AddressHandlerNear : IAddressHandler {
    override val blockchainType = BlockchainType.Near

    override fun isSupported(value: String): Boolean = when (NearKit.accountIdType(value)) {
        AccountId.Type.NearImplicit, AccountId.Type.EthImplicit -> true
        AccountId.Type.Named -> value.endsWith(".near")
        AccountId.Type.Deterministic, null -> false
    }

    override fun parseAddress(value: String): Address = Address(value, blockchainType = blockchainType)
}

/**
 * Claims any dotted NEAR named account (`alice.tg`), for fields that only take NEAR addresses,
 * where such a name can mean nothing else. A bare single-label name such as `alice` is a valid id
 * too, but also a plausible word in any other field, so it is not claimed here.
 */
class AddressHandlerNearName : IAddressHandler {
    override val blockchainType = BlockchainType.Near

    override fun isSupported(value: String): Boolean = when (NearKit.accountIdType(value)) {
        AccountId.Type.NearImplicit, AccountId.Type.EthImplicit -> true
        AccountId.Type.Named -> value.contains('.')
        AccountId.Type.Deterministic, null -> false
    }

    override fun parseAddress(value: String): Address = Address(value, blockchainType = blockchainType)
}
