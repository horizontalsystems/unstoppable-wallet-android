package cash.p.terminal.tangem.domain.card

import cash.p.terminal.tangem.domain.getSupportedCurves
import com.tangem.common.card.EllipticCurve
import io.horizontalsystems.core.entities.BlockchainType
import java.util.logging.Logger

object EdSingleCurrencyCardConfig : CardConfig {

    private val logger = Logger.getLogger("EdSingleCurrencyCardConfig")

    override val mandatoryCurves: List<EllipticCurve> = listOf(EllipticCurve.Ed25519)

    override fun primaryCurve(blockchain: BlockchainType): EllipticCurve? {
        return when {
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
