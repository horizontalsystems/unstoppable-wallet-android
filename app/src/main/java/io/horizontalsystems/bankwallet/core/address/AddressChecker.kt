package io.horizontalsystems.bankwallet.core.address

import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.SpamManager
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.subscriptions.core.AddressBlacklist
import io.horizontalsystems.subscriptions.core.AddressPhishing
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager

interface AddressChecker {
    suspend fun checkAddress(address: Address, token: Token): AddressCheckResult
    fun supports(token: Token): Boolean
}

class PhishingAddressChecker(
    private val spamManager: SpamManager
) : AddressChecker {

    override suspend fun checkAddress(address: Address, token: Token): AddressCheckResult {
        return when {
            !EvmBlockchainManager.blockchainTypes.contains(token.blockchainType) -> AddressCheckResult.NotSupported
            !UserSubscriptionManager.isActionAllowed(AddressPhishing) -> AddressCheckResult.NotAllowed
            else -> try {
                val spamAddress = spamManager.find(address.hex.uppercase())
                if (spamAddress != null)
                    AddressCheckResult.Detected
                else
                    AddressCheckResult.Clear
            } catch (e: Exception) {
                AddressCheckResult.NotAvailable
            }
        }
    }

    override fun supports(token: Token): Boolean {
        return EvmBlockchainManager.blockchainTypes.contains(token.blockchainType)
    }
}

class BlacklistAddressChecker : AddressChecker {
    override suspend fun checkAddress(address: Address, token: Token): AddressCheckResult {
        if (!UserSubscriptionManager.isActionAllowed(AddressBlacklist)) return AddressCheckResult.NotAllowed

        return AddressCheckResult.NotAvailable
    }

    override fun supports(token: Token): Boolean {
        return false
    }
}

class SanctionAddressChecker(
    private val chainalysisAddressValidator: ChainalysisAddressValidator
) : AddressChecker {
    override suspend fun checkAddress(address: Address, token: Token): AddressCheckResult {
        if (!UserSubscriptionManager.isActionAllowed(AddressBlacklist))
            return AddressCheckResult.NotAllowed

        return try {
            val identifications = chainalysisAddressValidator.check(address)
            if (identifications.isNotEmpty())
                AddressCheckResult.Detected
            else
                AddressCheckResult.Clear
        } catch (e: Exception) {
            AddressCheckResult.NotAvailable
        }
    }

    override fun supports(token: Token): Boolean {
        return true
    }
}
