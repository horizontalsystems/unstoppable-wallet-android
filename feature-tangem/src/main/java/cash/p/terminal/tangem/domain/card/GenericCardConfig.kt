package cash.p.terminal.tangem.domain.card

import cash.p.terminal.tangem.domain.getSupportedCurves
import com.tangem.common.card.EllipticCurve
import io.horizontalsystems.core.entities.BlockchainType
import java.util.logging.Logger

class GenericCardConfig(maxWalletCount: Int) : CardConfig {

    private val logger = Logger.getLogger("GenericCardConfig")

    override val mandatoryCurves: List<EllipticCurve> = buildList {
        add(EllipticCurve.Secp256k1)
        if (maxWalletCount > 1) {
            add(EllipticCurve.Ed25519)
        }
    }

    /**
     * Old logic to determine primary curve for blockchain
     */
    override fun primaryCurve(blockchain: BlockchainType): EllipticCurve? {
        return when {
            blockchain.getSupportedCurves().contains(EllipticCurve.Secp256k1) -> {
                EllipticCurve.Secp256k1
            }

            blockchain.getSupportedCurves().contains(EllipticCurve.Ed25519) -> {
                EllipticCurve.Ed25519
            }

            else -> {
                logger.warning("Unsupported blockchain, curve not found: $blockchain")
                null
            }
        }
    }
}
