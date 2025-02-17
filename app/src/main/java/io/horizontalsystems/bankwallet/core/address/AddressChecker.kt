package io.horizontalsystems.bankwallet.core.address

import HashDitAddressValidator
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
            } catch (e: Throwable) {
                AddressCheckResult.NotAvailable
            }
        }
    }

    override fun supports(token: Token): Boolean {
        return EvmBlockchainManager.blockchainTypes.contains(token.blockchainType)
    }
}

class BlacklistAddressChecker(
    private val hashDitAddressValidator: HashDitAddressValidator,
    private val eip20AddressValidator: Eip20AddressValidator
) : AddressChecker {
    override suspend fun checkAddress(address: Address, token: Token): AddressCheckResult {
        if (!UserSubscriptionManager.isActionAllowed(AddressBlacklist)) return AddressCheckResult.NotAllowed

        return try {
            val hashDitCheckResult = hashDitAddressValidator.check(address, token)
            val eip20CheckResult = eip20AddressValidator.check(address, token)

            val checkResults = listOf(hashDitCheckResult, eip20CheckResult)

            when {
                checkResults.contains(AddressCheckResult.Detected) -> {
                    AddressCheckResult.Detected
                }

                checkResults.contains(AddressCheckResult.Clear) &&
                        checkResults.all { it == AddressCheckResult.Clear || it == AddressCheckResult.NotSupported } -> {
                    AddressCheckResult.Clear
                }

                else -> {
                    AddressCheckResult.NotAvailable
                }
            }
        } catch (e: Throwable) {
            AddressCheckResult.NotAvailable
        }
    }

    override fun supports(token: Token): Boolean {
        return hashDitAddressValidator.supports(token) || eip20AddressValidator.supports(token)
    }
}

class SanctionAddressChecker(
    private val chainalysisAddressValidator: ChainalysisAddressValidator
) : AddressChecker {
    override suspend fun checkAddress(address: Address, token: Token): AddressCheckResult {
        if (!UserSubscriptionManager.isActionAllowed(AddressBlacklist))
            return AddressCheckResult.NotAllowed

        return try {
            chainalysisAddressValidator.check(address)
        } catch (e: Throwable) {
            AddressCheckResult.NotAvailable
        }
    }

    override fun supports(token: Token): Boolean {
        return true
    }
}
