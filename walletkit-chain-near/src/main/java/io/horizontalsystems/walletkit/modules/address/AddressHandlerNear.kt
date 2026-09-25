package io.horizontalsystems.walletkit.modules.address

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.nearkit.NearKit
import io.horizontalsystems.nearkit.crypto.AccountId
import io.horizontalsystems.walletkit.entities.Address

/**
 * Recognizes NEAR account ids: implicit (64 hex), ETH-implicit (`0x` + 40 hex) and named ones
 * with a top-level suffix (`alice.near`, `bob.tg`). A bare single-label name such as `alice`
 * is a valid id too, but also a plausible word in any other field, so it is not claimed here.
 */
class AddressHandlerNear : IAddressHandler {
    override val blockchainType = BlockchainType.Near

    override fun isSupported(value: String): Boolean = when (NearKit.accountIdType(value)) {
        AccountId.Type.NearImplicit, AccountId.Type.EthImplicit -> true
        AccountId.Type.Named -> value.contains('.')
        AccountId.Type.Deterministic, null -> false
    }

    override fun parseAddress(value: String): Address = Address(value, blockchainType = blockchainType)
}
