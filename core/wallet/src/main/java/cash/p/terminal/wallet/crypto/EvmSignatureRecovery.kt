package cash.p.terminal.wallet.crypto

import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.horizontalsystems.ethereumkit.models.Signature
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import io.horizontalsystems.hdwalletkit.ECKey
import org.bouncycastle.math.ec.ECAlgorithms
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve
import java.math.BigInteger
import java.security.SignatureException

/**
 * EVM signature recovery shared by hardware-wallet signers (Trezor, Tangem). Hardware devices
 * return only the raw `(r, s)` (and sometimes `v`) of a signature, so we have to rebuild the
 * signing hash ourselves and recover the public key / sender address from it — either to find the
 * recovery id (Tangem returns no `v`) or to verify the device signed with the expected account.
 */
object EvmSignatureRecovery {

    private const val EIP155_V_OFFSET = 35
    private const val PRE_EIP155_V_OFFSET = 27
    private const val UNCOMPRESSED_KEY_PREFIX_SIZE = 1
    private const val ADDRESS_BYTE_OFFSET = 12
    private val EIP1559_TX_TYPE = byteArrayOf(0x02)

    /** Keccak-256 of the transaction's signing preimage (EIP-155 for legacy, typed for EIP-1559). */
    fun signingHash(rawTransaction: RawTransaction, chainId: Int): ByteArray =
        when (val gasPrice = rawTransaction.gasPrice) {
            is GasPrice.Legacy -> CryptoUtils.sha3(
                RLP.encodeList(
                    RLP.encodeLong(rawTransaction.nonce),
                    RLP.encodeLong(gasPrice.legacyGasPrice),
                    RLP.encodeLong(rawTransaction.gasLimit),
                    RLP.encodeElement(rawTransaction.to.raw),
                    RLP.encodeBigInteger(rawTransaction.value),
                    RLP.encodeElement(rawTransaction.data),
                    RLP.encodeInt(chainId),
                    RLP.encodeElement(ByteArray(0)),
                    RLP.encodeElement(ByteArray(0))
                )
            )

            is GasPrice.Eip1559 -> CryptoUtils.sha3(
                EIP1559_TX_TYPE + RLP.encodeList(
                    RLP.encodeInt(chainId),
                    RLP.encodeLong(rawTransaction.nonce),
                    RLP.encodeLong(gasPrice.maxPriorityFeePerGas),
                    RLP.encodeLong(gasPrice.maxFeePerGas),
                    RLP.encodeLong(rawTransaction.gasLimit),
                    RLP.encodeElement(rawTransaction.to.raw),
                    RLP.encodeBigInteger(rawTransaction.value),
                    RLP.encodeElement(rawTransaction.data),
                    RLP.encode(arrayOf<Any>())
                )
            )
        }

    /** Recovers the sender address of a signed transaction, or null if it cannot be recovered. */
    fun recoverSenderAddress(
        rawTransaction: RawTransaction,
        signature: Signature,
        chainId: Int
    ): Address? {
        val recId = recoveryId(signature.v, chainId, rawTransaction.gasPrice)
        if (recId < 0) return null
        val publicKey = recoverPublicKey(
            recId = recId,
            r = BigInteger(1, signature.r),
            s = BigInteger(1, signature.s),
            messageHash = signingHash(rawTransaction, chainId),
            compressed = false
        ) ?: return null
        val keyWithoutPrefix = publicKey.copyOfRange(UNCOMPRESSED_KEY_PREFIX_SIZE, publicKey.size)
        return Address(CryptoUtils.sha3(keyWithoutPrefix).copyOfRange(ADDRESS_BYTE_OFFSET, 32))
    }

    /** Brute-forces the recovery id by matching the recovered public key against the expected one. */
    fun findRecoveryId(
        messageHash: ByteArray,
        r: BigInteger,
        s: BigInteger,
        expectedPublicKeyBytes: ByteArray
    ): Int {
        val compressed = isPubKeyCompressed(expectedPublicKeyBytes)
        for (recId in 0..3) {
            val publicKey = tryRecoverPublicKey(recId, r, s, messageHash, compressed)
            if (publicKey != null && publicKey.contentEquals(expectedPublicKeyBytes)) return recId
        }
        return -1
    }

    fun isPubKeyCompressed(encoded: ByteArray): Boolean = when {
        encoded.size == 32 ||
            (encoded.size == 33 && (encoded[0].toInt() == 0x02 || encoded[0].toInt() == 0x03)) -> true

        encoded.size == 65 && encoded[0].toInt() == 0x04 -> false
        else -> throw IllegalArgumentException("Unexpected public key size: ${encoded.size}")
    }

    private fun tryRecoverPublicKey(
        recId: Int,
        r: BigInteger,
        s: BigInteger,
        messageHash: ByteArray,
        compressed: Boolean
    ): ByteArray? = try {
        recoverPublicKey(recId, r, s, messageHash, compressed)
    } catch (_: SignatureException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun recoverPublicKey(
        recId: Int,
        r: BigInteger,
        s: BigInteger,
        messageHash: ByteArray,
        compressed: Boolean
    ): ByteArray? {
        val n = ECKey.ecParams.n
        val curve = ECKey.ecParams.curve as SecP256K1Curve

        val i = BigInteger.valueOf(recId.toLong() / 2)
        val x = r.add(i.multiply(n))
        if (x >= curve.q) return null

        val pointR = decompressKey(x, (recId and 1) == 1)
        if (!pointR.multiply(n).isInfinity) return null

        val e = BigInteger(1, messageHash)
        val eInv = BigInteger.ZERO.subtract(e).mod(n)
        val rInv = r.modInverse(n)
        val srInv = rInv.multiply(s).mod(n)
        val eInvrInv = rInv.multiply(eInv).mod(n)
        val q = ECAlgorithms.sumOfTwoMultiplies(ECKey.ecParams.g, eInvrInv, pointR, srInv)

        return ECKey(q.getEncoded(compressed)).pubKey
    }

    private fun decompressKey(xBN: BigInteger, yBit: Boolean): ECPoint {
        val curve = ECKey.ecParams.curve as SecP256K1Curve
        val x = curve.fromBigInteger(xBN)
        val alpha = x.multiply(x.square().add(curve.a)).add(curve.b)
        val beta = requireNotNull(alpha.sqrt()) { "Invalid point compression" }
        val nBeta = beta.toBigInteger()
        return if (nBeta.testBit(0) == yBit) {
            curve.createPoint(x.toBigInteger(), nBeta)
        } else {
            val y = curve.fromBigInteger(curve.q.subtract(nBeta))
            curve.createPoint(x.toBigInteger(), y.toBigInteger())
        }
    }

    private fun recoveryId(v: Int, chainId: Int, gasPrice: GasPrice): Int = when (gasPrice) {
        is GasPrice.Eip1559 -> if (v == 0 || v == 1) v else -1
        is GasPrice.Legacy -> {
            val eip155RecId = v - (EIP155_V_OFFSET + 2 * chainId)
            when {
                eip155RecId == 0 || eip155RecId == 1 -> eip155RecId
                v == PRE_EIP155_V_OFFSET || v == PRE_EIP155_V_OFFSET + 1 -> v - PRE_EIP155_V_OFFSET
                else -> -1
            }
        }
    }
}
