package cash.p.terminal.tangem.domain.card

import cash.p.terminal.tangem.domain.getSupportedCurves
import com.tangem.common.card.EllipticCurve
import io.horizontalsystems.core.entities.BlockchainType
import java.util.logging.Logger

data object Wallet2CardConfig : CardConfig {

    private val logger = Logger.getLogger("Wallet2CardConfig")

    override val mandatoryCurves: List<EllipticCurve>
        get() = listOf(
            EllipticCurve.Secp256k1,
            EllipticCurve.Ed25519,
            EllipticCurve.Bls12381G2Aug,
            EllipticCurve.Bip0340,
            EllipticCurve.Ed25519Slip0010,
        )

    /**
     * Logic to determine primary curve for blockchain in TangemWallet 2.0
     * Order is important here
     */
    override fun primaryCurve(blockchain: BlockchainType): EllipticCurve? {
        // order is important, new curve is preferred for wallet 2
        // TODO Comment old logic without direct mapping until tests and release
        // return when {
        //     blockchain.getSupportedCurves().contains(EllipticCurve.Ed25519Slip0010) -> {
        //         EllipticCurve.Ed25519Slip0010
        //     }
        //     blockchain.getSupportedCurves().contains(EllipticCurve.Secp256k1) -> {
        //         EllipticCurve.Secp256k1
        //     }
        //     blockchain.getSupportedCurves().contains(EllipticCurve.Bls12381G2Aug) -> {
        //         EllipticCurve.Bls12381G2Aug
        //     }
        //     // only for support cardano on Wallet2
        //     blockchain.getSupportedCurves().contains(EllipticCurve.Ed25519) -> {
        //         EllipticCurve.Ed25519
        //     }
        //     else -> {
        //         Timber.e("Unsupported blockchain, curve not found")
        //         null
        //     }
        // }
        val curve = getPrimaryCurveForBlockchain(blockchain)
        // check curve supports
        if (!blockchain.getSupportedCurves().contains(curve)) {
            logger.warning("Unsupported curve $curve for blockchain $blockchain")
            return null
        }
        return curve
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun getPrimaryCurveForBlockchain(blockchain: BlockchainType): EllipticCurve? {
        return when (blockchain) {
            BlockchainType.ArbitrumOne -> EllipticCurve.Secp256k1
            BlockchainType.Avalanche -> EllipticCurve.Secp256k1
            BlockchainType.ZkSync,
            BlockchainType.ECash,
            BlockchainType.BinanceSmartChain -> EllipticCurve.Secp256k1

            BlockchainType.Bitcoin -> EllipticCurve.Secp256k1
            BlockchainType.BitcoinCash -> EllipticCurve.Secp256k1
            BlockchainType.Dogecoin -> EllipticCurve.Secp256k1
            BlockchainType.Ethereum -> EllipticCurve.Secp256k1
            BlockchainType.Fantom -> EllipticCurve.Secp256k1
            BlockchainType.Litecoin -> EllipticCurve.Secp256k1
            BlockchainType.Polygon -> EllipticCurve.Secp256k1
            BlockchainType.Solana -> EllipticCurve.Ed25519Slip0010
            BlockchainType.Tron -> EllipticCurve.Secp256k1
            BlockchainType.Gnosis -> EllipticCurve.Secp256k1
            BlockchainType.Cosanta,
            BlockchainType.PirateCash,
            BlockchainType.Dash -> EllipticCurve.Secp256k1

            BlockchainType.Optimism -> EllipticCurve.Secp256k1
            BlockchainType.Ton -> EllipticCurve.Ed25519Slip0010
            BlockchainType.Base -> EllipticCurve.Secp256k1
            BlockchainType.Zcash,
            BlockchainType.Monero,
            BlockchainType.Stellar,
            is BlockchainType.Unsupported -> null
        }
    }
}