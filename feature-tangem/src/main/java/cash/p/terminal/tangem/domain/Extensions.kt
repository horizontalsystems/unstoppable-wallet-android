package cash.p.terminal.tangem.domain

import cash.p.terminal.wallet.entities.TokenType
import com.tangem.crypto.hdWallet.DerivationNode
import com.tangem.crypto.hdWallet.DerivationPath
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils.CURVE
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils.HALF_CURVE_ORDER
import io.horizontalsystems.hdwalletkit.ECDSASignature
import io.horizontalsystems.hdwalletkit.HDWallet

internal fun ECDSASignature.canonicalise() = if (s > HALF_CURVE_ORDER) {
    ECDSASignature(r, CURVE.n.subtract(s))
} else {
    this
}

internal fun TokenType.getPurpose(): String {
    return when (this) {
        is TokenType.Derived -> {
            when (derivation) {
                TokenType.Derivation.Bip44 -> "44'"
                TokenType.Derivation.Bip49 -> "49'"
                TokenType.Derivation.Bip84 -> "84'"
                TokenType.Derivation.Bip86 -> "86'"
            }
        }

        else -> ""
    }
}

/***
 * Replaces the first segment of the derivation path with a new purpose.
 * @param purpose can be "44'", "49", "84", etc. (hardened or non-hardened)
 */
internal fun DerivationPath.replacePurpose(purpose: String): DerivationPath? {
    if (purpose.isEmpty()) return null

    return if (purpose.last() == '\'') {
        val hardenedPurpose = purpose.dropLast(1).toLongOrNull() ?: return null
        replacePurpose(hardenedPurpose, isHardened = true)
    } else {
        val nonHardenedPurpose = purpose.toLongOrNull() ?: return null
        replacePurpose(nonHardenedPurpose, isHardened = false)
    }
}

internal fun DerivationPath.replacePurpose(purpose: Long, isHardened: Boolean): DerivationPath {
    val segments = nodes.toMutableList()
    if (segments.isNotEmpty()) {
        segments[0] = if (isHardened) {
            DerivationNode.Hardened(purpose)
        } else {
            DerivationNode.NonHardened(purpose)
        }
    }
    return DerivationPath(segments)
}

internal fun DerivationPath.getPurpose(): HDWallet.Purpose? {
    val segments = nodes.toMutableList()
    return if (segments.isNotEmpty()) {
        return when (segments[0].pathDescription.removeSuffix("'").toIntOrNull()) {
            HDWallet.Purpose.BIP44.value -> HDWallet.Purpose.BIP44
            HDWallet.Purpose.BIP49.value -> HDWallet.Purpose.BIP49
            HDWallet.Purpose.BIP84.value -> HDWallet.Purpose.BIP84
            HDWallet.Purpose.BIP86.value -> HDWallet.Purpose.BIP86
            else -> null
        }
    } else {
        null
    }
}
