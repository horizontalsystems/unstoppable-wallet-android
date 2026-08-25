package io.horizontalsystems.walletkit.core.address

import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.core.managers.SpamManager
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token

interface AddressChecker {
    suspend fun isClear(address: Address, token: Token): Boolean
    fun supports(token: Token): Boolean
}

class PhishingAddressChecker(
    private val spamManager: SpamManager
) : AddressChecker {

    private val supportedBlockchainTypes =  EvmBlockchainManager.blockchainTypes + listOf(BlockchainType.Tron, BlockchainType.Stellar, BlockchainType.Solana)

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
    private val chainCheckers: List<AddressChecker>,
) : AddressChecker {
    override suspend fun isClear(address: Address, token: Token): Boolean {
        for (checker in chainCheckers) {
            if (checker.supports(token) && !checker.isClear(address, token)) {
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
        return hashDitAddressValidator.supports(token) || chainCheckers.any { it.supports(token) }
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
