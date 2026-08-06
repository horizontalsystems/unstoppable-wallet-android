package io.horizontalsystems.walletkit.chain.solana

import io.horizontalsystems.bitcoincore.crypto.Base58
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.address.IAddressHandler
import io.horizontalsystems.walletkit.modules.send.address.EnterAddressValidator
import io.horizontalsystems.marketkit.models.BlockchainType

class AddressHandlerSolana : IAddressHandler {
    override fun isSupported(value: String): Boolean {
        return try {
            //then count size to validate address length
            //Solana address should be 32 bytes long
            val bytes = Base58.decode(value)
            if (bytes.size != 32) throw IllegalStateException()

            io.horizontalsystems.solanakit.models.Address(value)
            true
        } catch (e: Throwable) {
            false
        }
    }

    override val blockchainType = BlockchainType.Solana

    override fun parseAddress(value: String): Address {
        try {
            //simulate steps in Solana kit init
            io.horizontalsystems.solanakit.models.Address(value)
        } catch (e: Throwable) {
            throw AddressValidator.AddressValidationException(e.message ?: "")
        }

        return Address(value, blockchainType = blockchainType)
    }

}

class SolanaAddressValidator : EnterAddressValidator {
    override suspend fun validate(address: Address) {
        io.horizontalsystems.solanakit.models.Address(address.hex)
    }
}
