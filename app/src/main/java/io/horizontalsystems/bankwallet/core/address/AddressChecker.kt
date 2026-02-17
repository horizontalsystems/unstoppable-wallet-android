package io.horizontalsystems.bankwallet.core.address

import HashDitAddressValidator
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.SpamManager
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token

interface AddressChecker {
    suspend fun isClear(address: Address, token: Token): Boolean
    fun supports(token: Token): Boolean
}

class PhishingAddressChecker(
    private val spamManager: SpamManager
) : AddressChecker {

    private val supportedBlockchainTypes =  EvmBlockchainManager.blockchainTypes + listOf(BlockchainType.Tron, BlockchainType.Stellar)

    override suspend fun isClear(address: Address, token: Token): Boolean {
        val spamTransaction = spamManager.findSpamByAddress(address.hex)
        return spamTransaction == null
    }

    override fun supports(token: Token): Boolean {
        return supportedBlockchainTypes.contains(token.blockchainType)
    }
}

class BlacklistAddressChecker(
    private val hashDitAddressValidator: HashDitAddressValidator,
    private val eip20AddressValidator: Eip20AddressValidator,
    private val trc20AddressValidator: Trc20AddressValidator,
) : AddressChecker {
    override suspend fun isClear(address: Address, token: Token): Boolean {
        if (token.blockchainType == BlockchainType.Tron) {
            return trc20AddressValidator.isClear(address, token)
        }
        if (eip20AddressValidator.supports(token)) {
            if (!eip20AddressValidator.isClear(address, token)) {
                return false
            }
        }
        if (hashDitAddressValidator.supports(token)) {
            if (!hashDitAddressValidator.isClear(address, token)) {
                return false
            }
        }
        return true
    }

    override fun supports(token: Token): Boolean {
        if(token.blockchainType == BlockchainType.Tron) {
            return trc20AddressValidator.supports(token)
        }
        return hashDitAddressValidator.supports(token) || eip20AddressValidator.supports(token)
    }
}

class SanctionAddressChecker(
    private val chainalysisAddressValidator: ChainalysisAddressValidator
) : AddressChecker {
    override suspend fun isClear(address: Address, token: Token): Boolean {
        return chainalysisAddressValidator.isClear(address)
    }

    override fun supports(token: Token): Boolean {
        return true
    }
}
