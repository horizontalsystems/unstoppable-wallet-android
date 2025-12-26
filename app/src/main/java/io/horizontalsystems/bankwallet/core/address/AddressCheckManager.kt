package io.horizontalsystems.bankwallet.core.address

import HashDitAddressValidator
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
import io.horizontalsystems.bankwallet.core.managers.SpamManager
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.marketkit.models.Token

class AddressCheckManager(
    spamManager: SpamManager,
    appConfigProvider: AppConfigProvider,
    evmBlockchainManager: EvmBlockchainManager,
    evmSyncSourceManager: EvmSyncSourceManager
) {
    private val checkers = mapOf(
        AddressCheckType.Phishing to PhishingAddressChecker(spamManager),
        AddressCheckType.Blacklist to BlacklistAddressChecker(
            HashDitAddressValidator(
                appConfigProvider.hashDitBaseUrl,
                appConfigProvider.hashDitApiKey,
                evmBlockchainManager
            ),
            Eip20AddressValidator(evmSyncSourceManager),
            Trc20AddressValidator()
        ),
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

    suspend fun isClear(type: AddressCheckType, address: Address, token: Token): Boolean {
        return checkers[type]?.isClear(address, token) ?: true
    }
}
