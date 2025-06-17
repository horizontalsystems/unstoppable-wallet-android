package cash.p.terminal.tangem.signer

import cash.p.terminal.tangem.domain.canonicalise
import cash.p.terminal.tangem.domain.usecase.SignOneHashTransactionUseCase
import cash.p.terminal.wallet.entities.HardwarePublicKey
import com.tangem.common.CompletionResult
import com.tangem.crypto.hdWallet.DerivationPath
import io.horizontalsystems.hdwalletkit.ECDSASignature
import io.horizontalsystems.hdwalletkit.ECKey
import io.horizontalsystems.tronkit.hexStringToByteArray
import io.horizontalsystems.tronkit.network.CreatedTransaction
import io.horizontalsystems.tronkit.transaction.Signer
import kotlinx.coroutines.runBlocking
import org.bouncycastle.math.ec.ECAlgorithms
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve
import org.bouncycastle.util.encoders.Hex
import org.koin.java.KoinJavaComponent.inject
import java.math.BigInteger
import java.security.SignatureException


class HardwareWalletTronSigner(
    private val hardwarePublicKey: HardwarePublicKey,
    private val cardId: String,
    private val expectedPublicKeyBytes: ByteArray
) : Signer(BigInteger.ZERO) {

    private val signOneHashTransactionUseCase: SignOneHashTransactionUseCase by inject(
        SignOneHashTransactionUseCase::class.java
    )

    override fun sign(createdTransaction: CreatedTransaction): ByteArray {
        val rawTransactionHash =
            io.horizontalsystems.hdwalletkit.Utils.sha256(createdTransaction.raw_data_hex.hexStringToByteArray())
        return runBlocking {
            val signBytesResponse =
                signOneHashTransactionUseCase(
                    hash = rawTransactionHash,
                    walletPublicKey = hardwarePublicKey.publicKey,
                    derivationPath = DerivationPath(hardwarePublicKey.derivationPath)
                )
            when (signBytesResponse) {
                is CompletionResult.Success -> {
                    val byteSignature = signBytesResponse.data.signature
                    val r = byteSignature.sliceArray(0..31)
                    val s = byteSignature.sliceArray(32..63)

                    val recoveryId = findRecoveryId(
                        messageHash = rawTransactionHash,
                        r = BigInteger(1, r),
                        s = BigInteger(1, s),
                        expectedPublicKeyBytes = expectedPublicKeyBytes
                    )

                    if (recoveryId == -1) {
                        throw SignatureException("Could not find valid recoveryId for the signature")
                    }

                    val v = recoveryId + 27

                    r + s + byteArrayOf(v.toByte())
                }

                is CompletionResult.Failure -> {
                    throw Exception("Signing failed: ${signBytesResponse.error}")
                }
            }
        }
    }

    private fun findRecoveryId(
        messageHash: ByteArray,
        r: BigInteger,
        s: BigInteger,
        expectedPublicKeyBytes: ByteArray
    ): Int {
        val signature = ECDSASignature(r, s).canonicalise()

        //
        // Get the RecID used to recover the public key from the signature
        //
        val e = BigInteger(1, messageHash)
        for (recId in 0..3) {
            try {
                val key: ECKey? = recoverFromSignature(
                    recID = recId,
                    sig = signature,
                    e = e,
                    compressed = isPubKeyCompressed(expectedPublicKeyBytes)
                )
                if (key != null && key.pubKey.contentEquals(expectedPublicKeyBytes)) {
                    return recId
                }
            } catch (e: SignatureException) {
                System.err.println("Error restoring recId $recId: ${e.message}")
                continue
            } catch (e: IllegalArgumentException) {
                System.err.println("Wrong arguments recId $recId: ${e.message}")
                continue
            }
        }
        return -1
    }

    private fun recoverFromSignature(
        recID: Int,
        sig: ECDSASignature,
        e: BigInteger?,
        compressed: Boolean
    ): ECKey? {
        val n = ECKey.ecParams.n
        val i = BigInteger.valueOf(recID.toLong() / 2)
        val x = sig.r.add(i.multiply(n))
        //
        //   Convert the integer x to an octet string X of length mlen using the conversion routine
        //        specified in Section 2.3.7, where mlen = ⌈(log2 p)/8⌉ or mlen = ⌈m/8⌉.
        //   Convert the octet string (16 set binary digits)||X to an elliptic curve point R using the
        //        conversion routine specified in Section 2.3.4. If this conversion routine outputs 'invalid', then
        //        do another iteration.
        //
        // More concisely, what these points mean is to use X as a compressed public key.
        //
        val curve = ECKey.ecParams.curve as SecP256K1Curve
        val prime = curve.q
        if (x >= prime) {
            return null
        }
        //
        // Compressed keys require you to know an extra bit of data about the y-coordinate as
        // there are two possibilities.  So it's encoded in the recID.
        //
        val R = decompressKey(x, (recID and 1) == 1)
        if (!R.multiply(n).isInfinity) return null
        //
        //   For k from 1 to 2 do the following.   (loop is outside this function via iterating recId)
        //     Compute a candidate public key as:
        //       Q = mi(r) * (sR - eG)
        //
        // Where mi(x) is the modular multiplicative inverse. We transform this into the following:
        //               Q = (mi(r) * s ** R) + (mi(r) * -e ** G)
        // Where -e is the modular additive inverse of e, that is z such that z + e = 0 (mod n).
        // In the above equation, ** is point multiplication and + is point addition (the EC group operator).
        //
        // We can find the additive inverse by subtracting e from zero then taking the mod. For example the additive
        // inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and -3 mod 11 = 8.
        //
        val eInv = BigInteger.ZERO.subtract(e).mod(n)
        val rInv = sig.r.modInverse(n)
        val srInv = rInv.multiply(sig.s).mod(n)
        val eInvrInv = rInv.multiply(eInv).mod(n)
        val q = ECAlgorithms.sumOfTwoMultiplies(ECKey.ecParams.g, eInvrInv, R, srInv)

        return ECKey(q.getEncoded(compressed))
    }

    private fun decompressKey(xBN: BigInteger?, yBit: Boolean): ECPoint {
        val curve = ECKey.ecParams.getCurve() as SecP256K1Curve
        val x = curve.fromBigInteger(xBN)
        val alpha = x.multiply(x.square().add(curve.getA())).add(curve.getB())
        val beta = alpha.sqrt()
        requireNotNull(beta) { "Invalid point compression" }
        val ecPoint: ECPoint
        val nBeta = beta.toBigInteger()
        if (nBeta.testBit(0) == yBit) {
            ecPoint = curve.createPoint(x.toBigInteger(), nBeta)
        } else {
            val y = curve.fromBigInteger(curve.getQ().subtract(nBeta))
            ecPoint = curve.createPoint(x.toBigInteger(), y.toBigInteger())
        }
        return ecPoint
    }

    fun isPubKeyCompressed(encoded: ByteArray): Boolean {
        return if (encoded.size == 32 || (encoded.size == 33 && (encoded[0].toInt() == 0x02 || encoded[0].toInt() == 0x03))) true
        else if (encoded.size == 65 && encoded[0].toInt() == 0x04) false
        else throw java.lang.IllegalArgumentException(Hex.toHexString(encoded))
    }
}
