package cash.p.terminal.tangem.domain

import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.common.extensions.toDecompressedPublicKey
import io.horizontalsystems.core.entities.BlockchainType

fun BlockchainType.getSupportedCurves(): List<EllipticCurve> {
    return when (this) {
        BlockchainType.Zcash,
        is BlockchainType.Unsupported -> emptyList()
        /*Tezos,
            -> listOf(
            EllipticCurve.Secp256k1,
            EllipticCurve.Ed25519,
            EllipticCurve.Ed25519Slip0010,
        )

        XRP,
            -> listOf(EllipticCurve.Secp256k1, EllipticCurve.Ed25519)
*/
        BlockchainType.ArbitrumOne,
        BlockchainType.Bitcoin,
        BlockchainType.BitcoinCash,
        BlockchainType.BinanceChain, BlockchainType.BinanceSmartChain,
        BlockchainType.Ethereum,
        BlockchainType.Gnosis,
        BlockchainType.Polygon,
        BlockchainType.Avalanche,
        BlockchainType.Fantom,
        BlockchainType.Litecoin,
        BlockchainType.Dogecoin,
        BlockchainType.Tron,
        BlockchainType.Cosanta,
        BlockchainType.PirateCash,
        BlockchainType.Dash,
        BlockchainType.ECash,
        BlockchainType.Optimism,
        BlockchainType.ZkSync,
        BlockchainType.Base,
            -> listOf(EllipticCurve.Secp256k1)

        BlockchainType.Solana,
        BlockchainType.Ton,
            -> listOf(EllipticCurve.Ed25519, EllipticCurve.Ed25519Slip0010)

//        Cardano -> listOf(EllipticCurve.Ed25519) // todo until cardano support in wallet 2

//        Chia, ChiaTestnet,
//            -> listOf(EllipticCurve.Bls12381G2Aug)
    }
}

internal fun BlockchainType.preparePublicKeyByType(data: ByteArray): ByteArray {
    return when (getSupportedCurves().firstOrNull()) {
        EllipticCurve.Secp256k1 -> data.toCompressedPublicKey()
//        PublicKeyType.SECP256K1EXTENDED -> data.toDecompressedPublicKey()
        else -> data
    }
}

fun BlockchainType.getCoinTypeString(): String {
    return when (this) {
        BlockchainType.Bitcoin -> "0'"
        BlockchainType.BitcoinCash -> "145'"
        BlockchainType.ECash -> "145'"
        BlockchainType.Litecoin -> "2'"
        BlockchainType.Dogecoin -> "3'"
        BlockchainType.Dash -> "5'"
        BlockchainType.Zcash -> "133'"
        BlockchainType.Ethereum -> "60'"
        BlockchainType.BinanceSmartChain -> "60'"
        BlockchainType.BinanceChain -> "714'"
        BlockchainType.Polygon -> "60'"
        BlockchainType.Avalanche -> "60'"
        BlockchainType.Optimism -> "60'"
        BlockchainType.ArbitrumOne -> "60'"
        BlockchainType.Solana -> "501'"
        BlockchainType.Gnosis -> "700'"
        BlockchainType.Fantom -> "60'"
        BlockchainType.Tron -> "195'"
        BlockchainType.Ton -> "607'"
        BlockchainType.Base -> "60'"
        BlockchainType.ZkSync -> "60'"
        BlockchainType.Cosanta -> "770'"
        BlockchainType.PirateCash -> "660'"
        is BlockchainType.Unsupported -> throw IllegalArgumentException("Unsupported blockchain uid: ${this._uid}")
    }
}