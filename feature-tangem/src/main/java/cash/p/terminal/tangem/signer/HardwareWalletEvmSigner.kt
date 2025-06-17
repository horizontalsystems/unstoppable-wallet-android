package cash.p.terminal.tangem.signer

import cash.p.terminal.tangem.domain.canonicalise
import cash.p.terminal.tangem.domain.usecase.SignOneHashTransactionUseCase
import cash.p.terminal.wallet.entities.HardwarePublicKey
import com.tangem.common.CompletionResult
import com.tangem.crypto.hdWallet.DerivationPath
import io.horizontalsystems.ethereumkit.core.TransactionBuilder
import io.horizontalsystems.ethereumkit.core.TransactionSigner
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.signer.EthSigner
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.crypto.EIP712Encoder
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.horizontalsystems.ethereumkit.models.Signature
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import io.horizontalsystems.hdwalletkit.ECDSASignature
import io.horizontalsystems.hdwalletkit.ECKey
import org.bouncycastle.math.ec.ECAlgorithms
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve
import org.bouncycastle.util.encoders.Hex
import org.koin.java.KoinJavaComponent.inject
import java.math.BigInteger
import java.security.SignatureException


class HardwareWalletEvmSigner(
    address: Address,
    private val publicKey: HardwarePublicKey,
    private val cardId: String,
    private val chain: Chain,
    private val expectedPublicKeyBytes: ByteArray
) : Signer(
    transactionBuilder = TransactionBuilder(address, chain.id),
    transactionSigner = TransactionSigner(MOCK_PRIVATE_KEY, chain.id),
    ethSigner = EthSigner(MOCK_PRIVATE_KEY, CryptoUtils, EIP712Encoder())
) {
    companion object {
        private val MOCK_PRIVATE_KEY = BigInteger("1")
    }

    private val signOneHashTransactionUseCase: SignOneHashTransactionUseCase by inject(
        SignOneHashTransactionUseCase::class.java
    )

    override suspend fun signature(rawTransaction: RawTransaction): Signature {
        return when (rawTransaction.gasPrice) {
            is GasPrice.Eip1559 -> {
                signatureEip1559(rawTransaction)
            }
            is GasPrice.Legacy -> {
                signEip155(rawTransaction)
            }
        }
    }

    private suspend fun signEip155(rawTransaction: RawTransaction): Signature {
        val gasPrice = rawTransaction.gasPrice as GasPrice.Legacy
        val encodedTransaction = RLP.encodeList(
            RLP.encodeLong(rawTransaction.nonce),
            RLP.encodeLong(gasPrice.legacyGasPrice),
            RLP.encodeLong(rawTransaction.gasLimit),
            RLP.encodeElement(rawTransaction.to.raw),
            RLP.encodeBigInteger(rawTransaction.value),
            RLP.encodeElement(rawTransaction.data),
            RLP.encodeInt(chain.id),
            RLP.encodeElement(ByteArray(0)),
            RLP.encodeElement(ByteArray(0)))

        val rawTransactionHash = CryptoUtils.sha3(encodedTransaction)

        return getSignature(
            cardId = cardId,
            hash = rawTransactionHash,
            walletPublicKey = publicKey.publicKey,
            expectedPublicKeyBytes = expectedPublicKeyBytes,
            isLegacy = true
        )
    }

    private suspend fun signatureEip1559(rawTransaction: RawTransaction): Signature {
        val gasPrice = rawTransaction.gasPrice as GasPrice.Eip1559
        val encodedTransaction = RLP.encodeList(
            RLP.encodeInt(chain.id),
            RLP.encodeLong(rawTransaction.nonce),
            RLP.encodeLong(gasPrice.maxPriorityFeePerGas),
            RLP.encodeLong(gasPrice.maxFeePerGas),
            RLP.encodeLong(rawTransaction.gasLimit),
            RLP.encodeElement(rawTransaction.to.raw),
            RLP.encodeBigInteger(rawTransaction.value),
            RLP.encodeElement(rawTransaction.data),
            RLP.encode(arrayOf<Any>())
        )
        val rawTransactionHash =
            CryptoUtils.sha3("0x02".hexStringToByteArray() + encodedTransaction)

        return getSignature(
            cardId = cardId,
            hash = rawTransactionHash,
            walletPublicKey = publicKey.publicKey,
            expectedPublicKeyBytes = expectedPublicKeyBytes,
            isLegacy = false
        )
    }

    private suspend fun getSignature(
        cardId: String?,
        hash: ByteArray,
        walletPublicKey: ByteArray,
        expectedPublicKeyBytes: ByteArray,
        isLegacy: Boolean
    ): Signature {
        val signBytesResponse =
            signOneHashTransactionUseCase(hash, walletPublicKey, DerivationPath(publicKey.derivationPath))
        when (signBytesResponse) {
            is CompletionResult.Success -> {
                val byteSignature = signBytesResponse.data.signature
                if (byteSignature.size != 64) {
                    throw IllegalArgumentException("Wrong signature size: ${byteSignature.size}")
                }
                val r = byteSignature.sliceArray(0..31)
                val s = byteSignature.sliceArray(32..63)
                val v = findRecoveryId(
                    messageHash = hash,
                    r = BigInteger(1, r),
                    s = BigInteger(1, s),
                    expectedPublicKeyBytes = expectedPublicKeyBytes
                )

                val finalV = if (isLegacy) {
                    v + if (chain.id == 0) 27 else (35 + 2 * chain.id)
                } else {
                    v
                }

                if (v == -1) {
                    throw SignatureException("Could not find valid recoveryId for the signature")
                }

                return Signature(v = finalV, r = r, s = s)
            }

            is CompletionResult.Failure -> {
                throw Exception("Signing failed: ${signBytesResponse.error}")
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
