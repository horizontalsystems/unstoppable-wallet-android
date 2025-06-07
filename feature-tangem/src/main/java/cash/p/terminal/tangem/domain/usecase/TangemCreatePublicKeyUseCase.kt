package cash.p.terminal.tangem.domain.usecase

import cash.p.terminal.tangem.domain.card.CardConfig
import cash.p.terminal.tangem.domain.card.Wallet2CardConfig
import cash.p.terminal.tangem.domain.getDerivationStyle
import cash.p.terminal.tangem.domain.getPurpose
import cash.p.terminal.tangem.domain.model.ScanResponse
import cash.p.terminal.wallet.entities.TokenType
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toMapKey
import io.horizontalsystems.core.entities.BlockchainType

class TangemCreatePublicKeyUseCase {
    operator fun invoke(scanResponse: ScanResponse, blockchainType: BlockchainType, tokenType: TokenType): ByteArray? {
        if (!scanResponse.card.settings.isHDWalletAllowed) return null
        val cardConfig = CardConfig.createConfig(scanResponse.card)

        val wallet = selectWallet(
            cardConfig = cardConfig,
            wallets = scanResponse.card.wallets,
            blockchainType = blockchainType,
        ) ?: return null

//        val seedKey = scanResponse.card.cardPublicKey
        val derivationStyle = scanResponse.card.getDerivationStyle() ?: return null
        val derivations = derivationStyle.getConfig().derivations(blockchainType, tokenType.getPurpose())
        val derivationPath = derivations.values.firstOrNull() ?: return null

        val derivedKeys = scanResponse.derivedKeys[wallet.publicKey.toMapKey()] ?: return null

        return derivedKeys[derivationPath]?.publicKey
        /*val publicKey = makePublicKey(
            seedKey = wallet.publicKey,
            blockchainType = blockchainType,
            derivationPath = derivationPath,
            derivedWalletKeys = derivedKeys
        )*/
    }

    private fun selectWallet(
        cardConfig: CardConfig,
        wallets: List<CardWallet>,
        blockchainType: BlockchainType,
    ): CardWallet? {
        return if (cardConfig is Wallet2CardConfig) {
            val primaryCurve = cardConfig.primaryCurve(blockchainType)
            wallets.firstOrNull { it.curve == primaryCurve }
        } else {
            when (wallets.size) {
                0 -> null
                1 -> wallets[0]
                else -> wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 } ?: wallets[0]
            }
        }
    }


    /*private fun makePublicKey(
        seedKey: ByteArray,
        blockchainType: BlockchainType,
        derivationPath: DerivationPath,
        derivedWalletKeys: Map<DerivationPath, ExtendedPublicKey>,
    ): ByteArray? {
        val derivedKey = derivedWalletKeys[derivationPath] ?: return null

        val derivationKey = Wallet.HDKey(
            path = derivationPath,
            extendedPublicKey = derivedKey,
        )


        return Wallet.PublicKey(
            seedKey = seedKey,
            derivationType = Wallet.PublicKey.DerivationType.Plain(derivationKey),
        )
    }
*/
}