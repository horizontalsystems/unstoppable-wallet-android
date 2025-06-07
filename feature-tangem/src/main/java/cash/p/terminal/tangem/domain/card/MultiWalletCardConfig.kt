package cash.p.terminal.tangem.domain.card

import cash.p.terminal.tangem.domain.getSupportedCurves
import com.tangem.common.card.EllipticCurve
import io.horizontalsystems.core.entities.BlockchainType
import java.util.logging.Logger

object MultiWalletCardConfig : CardConfig {

    private val logger = Logger.getLogger("MultiWalletCardConfig")

    override val mandatoryCurves: List<EllipticCurve>
        get() = listOf(
            EllipticCurve.Secp256k1,
            EllipticCurve.Ed25519,
            EllipticCurve.Bls12381G2Aug,
        )

    /**
     * Old logic to determine primary curve for blockchain in TangemWallet
     */
    override fun primaryCurve(blockchain: BlockchainType): EllipticCurve? {
        return when {
            blockchain.getSupportedCurves().contains(EllipticCurve.Secp256k1) -> {
                EllipticCurve.Secp256k1
            }

            blockchain.getSupportedCurves().contains(EllipticCurve.Ed25519) -> {
                EllipticCurve.Ed25519
            }

            blockchain.getSupportedCurves().contains(EllipticCurve.Bls12381G2Aug) -> {
                EllipticCurve.Bls12381G2Aug
            }

            else -> {
                logger.warning("Unsupported blockchain, curve not found: $blockchain")
                null
            }
        }
    }
}
