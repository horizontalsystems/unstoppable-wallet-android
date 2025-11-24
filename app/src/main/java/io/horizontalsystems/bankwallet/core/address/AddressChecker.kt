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
        val spamAddress = spamManager.find(address.hex)
        return spamAddress == null
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
        if (hashDitAddressValidator.supports(token)) {
            if (!hashDitAddressValidator.isClear(address, token)) {
                return false
            } else if (!eip20AddressValidator.supports(token)) {
                return true
            }
        }
        val eip20CheckResult = eip20AddressValidator.isClear(address, token)
        return eip20CheckResult
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
