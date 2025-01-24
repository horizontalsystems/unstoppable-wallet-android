package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.address.AddressSecurityCheckerChain
import io.horizontalsystems.bankwallet.core.address.AddressSecurityCheckerChain.IAddressSecurityCheckerItem
import io.horizontalsystems.bankwallet.core.address.ChainalysisAddressValidator
import io.horizontalsystems.bankwallet.core.address.SpamAddressDetector
import io.horizontalsystems.bankwallet.core.managers.SpamManager
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.supported
import io.horizontalsystems.marketkit.models.BlockchainType

class AddressSecurityCheckerFactory(
    private val spamManager: SpamManager,
    private val appConfigProvider: AppConfigProvider
) {

    private fun securityCheckerChainHandlers(blockchainType: BlockchainType): List<IAddressSecurityCheckerItem> =
        when (blockchainType) {
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Avalanche,
            BlockchainType.Optimism,
            BlockchainType.Base,
            BlockchainType.Gnosis,
            BlockchainType.Fantom,
            BlockchainType.ArbitrumOne -> {
                val evmAddressSecurityCheckerItem = SpamAddressDetector(spamManager)
                val chainalysisAddressValidator =
                    ChainalysisAddressValidator(appConfigProvider.chainalysisBaseUrl, appConfigProvider.chainalysisApiKey)
                val handlers = mutableListOf<IAddressSecurityCheckerItem>()
                handlers.add(evmAddressSecurityCheckerItem)
                handlers.add(chainalysisAddressValidator)
                handlers
            }

            else ->
                emptyList()
        }

    fun securityCheckerChain(blockchainType: BlockchainType?): AddressSecurityCheckerChain {
        if (blockchainType != null) {
            return AddressSecurityCheckerChain().append(securityCheckerChainHandlers(blockchainType))
        }

        val handlers = mutableListOf<IAddressSecurityCheckerItem>()
        for (supportedBlockchainType in BlockchainType.supported) {
            handlers.addAll(securityCheckerChainHandlers(supportedBlockchainType))
        }

        return AddressSecurityCheckerChain().append(handlers)
    }

}
