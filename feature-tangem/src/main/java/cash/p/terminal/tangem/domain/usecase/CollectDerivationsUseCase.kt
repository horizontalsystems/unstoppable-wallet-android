package cash.p.terminal.tangem.domain.usecase

import cash.p.terminal.tangem.domain.card.CardConfig
import cash.p.terminal.tangem.domain.derivation.DerivationConfig
import cash.p.terminal.tangem.domain.getDerivationStyle
import cash.p.terminal.tangem.domain.getPurpose
import cash.p.terminal.tangem.domain.model.BlockchainToDerive
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import com.tangem.common.card.Card
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import io.horizontalsystems.core.entities.BlockchainType

class CollectDerivationsUseCase {

    operator fun invoke(
        card: Card,
        config: CardConfig,
        blockchainsToDerive: List<TokenQuery>
    ): Map<ByteArrayKey, List<DerivationPath>> {
        val derivations = mutableMapOf<ByteArrayKey, List<DerivationPath>>()
        if (!card.settings.isHDWalletAllowed || card.wallets.isEmpty()) return derivations

        val derivationStyle = card.getDerivationStyle() ?: return derivations
        val blockchains = mapToBlockchainsToDerive(derivationStyle.getConfig(), blockchainsToDerive)

        blockchains.forEach { blockchain ->
            val curve = config.primaryCurve(blockchain.blockchainType)
            val wallet = card.wallets.firstOrNull { it.curve == curve } ?: return@forEach
            if (wallet.chainCode == null) return@forEach

            val key = wallet.publicKey.toMapKey()
            val path = blockchain.derivationPath
            if (path != null) {
                val addedDerivations = derivations[key]
                if (addedDerivations != null) {
                    derivations[key] = addedDerivations + path
                } else {
                    derivations[key] = listOf(path)
                }
            }
        }

        return derivations
    }

    private fun mapToBlockchainsToDerive(
        derivationConfig: DerivationConfig,
        blockchainsToDerive: List<TokenQuery>
    ) = blockchainsToDerive.mapNotNull { (blockchainType, tokenType) ->
        val derivationPath = derivationConfig.derivations(blockchainType, tokenType.getPurpose()).values.firstOrNull()
            ?: return@mapNotNull null
        BlockchainToDerive(blockchainType, derivationPath)
    }
}