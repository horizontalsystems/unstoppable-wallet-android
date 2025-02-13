package io.horizontalsystems.bankwallet.core.address

import io.horizontalsystems.bankwallet.core.managers.SpamManager
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.marketkit.models.Token

class AddressCheckManager(
    spamManager: SpamManager,
    appConfigProvider: AppConfigProvider
) {
    private val checkers = mapOf(
        AddressCheckType.Phishing to PhishingAddressChecker(spamManager),
        AddressCheckType.Blacklist to BlacklistAddressChecker(),
        AddressCheckType.Sanction to SanctionAddressChecker(
            ChainalysisAddressValidator(
                appConfigProvider.chainalysisBaseUrl,
                appConfigProvider.chainalysisApiKey
            )
        )
    )

    fun availableCheckTypes(token: Token): List<AddressCheckType> {
        return checkers.mapNotNull { (type, checker) -> if (checker.supports(token)) type else null }
    }

    suspend fun check(type: AddressCheckType, address: Address, token: Token): AddressCheckResult {
        return checkers[type]?.checkAddress(address, token) ?: AddressCheckResult.NotSupported
    }
}
