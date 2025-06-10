package cash.p.terminal.tangem.common

import cash.p.terminal.tangem.domain.model.AddressBytesWithPublicKey
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.hdwalletkit.Base58
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDKey
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.math.ec.ECCurve
import org.bouncycastle.math.ec.ECPoint
import java.security.MessageDigest
import kotlin.experimental.and

object CustomXPubKeyAddressParser {
    private const val LENGTH = 82

    fun parse(xPubKey: String): AddressBytesWithPublicKey {
        val version = HDExtendedKey.Companion.version(xPubKey)

        val data = Base58.decode(xPubKey)
        if (data.size != LENGTH) {
            throw HDExtendedKey.ParsingError.WrongKeyLength
        }

        val depth = data[4] and 0xff.toByte()

        var parentFingerprint = data[5].toInt() and 0x000000FF
        parentFingerprint = parentFingerprint shl 8
        parentFingerprint = parentFingerprint or (data[6].toInt() and 0x000000FF)
        parentFingerprint = parentFingerprint shl 8
        parentFingerprint = parentFingerprint or (data[7].toInt() and 0x000000FF)
        parentFingerprint = parentFingerprint shl 8
        parentFingerprint = parentFingerprint or (data[8].toInt() and 0x000000FF)

        var sequence = data[9].toInt() and 0x000000FF
        sequence = sequence shl 8
        sequence = sequence or (data[10].toInt() and 0x000000FF)
        sequence = sequence shl 8
        sequence = sequence or (data[11].toInt() and 0x000000FF)
        sequence = sequence shl 8
        sequence = sequence or (data[12].toInt() and 0x000000FF)

        val hardened = sequence and HDKey.HARDENED_FLAG != 0
        val childNumber = sequence and 0x7FFFFFFF

        val derivedType = HDExtendedKey.DerivedType.Companion.initFrom(depth.toInt())

        HDExtendedKey.Companion.validateChecksum(data)

        val bytes: ByteArray = data.copyOfRange(0, data.size - 4)
        val chainCode: ByteArray = bytes.copyOfRange(13, 13 + 32)
        val compressedPublicKeyBytes: ByteArray = bytes.copyOfRange(13 + 32, bytes.size)

        val sha256Digest = MessageDigest.getInstance("SHA-256")
        val ripemd160Digest = MessageDigest.getInstance("RIPEMD160", "BC")

        val sha256OfCompressedPubKey = sha256Digest.digest(compressedPublicKeyBytes)
        val hash160OfCompressedPubKey = ripemd160Digest.digest(sha256OfCompressedPubKey)
        val calculatedKeyFingerprintBytes = hash160OfCompressedPubKey.copyOfRange(0, 4)
        val calculatedKeyFingerprintHex = calculatedKeyFingerprintBytes.toHexString()

        val ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val curve: ECCurve = ecSpec.curve
        val ecPoint: ECPoint = curve.decodePoint(compressedPublicKeyBytes)

        val uncompressedPublicKeyWithPrefix = ecPoint.getEncoded(false)
        if (uncompressedPublicKeyWithPrefix.size != 65 || uncompressedPublicKeyWithPrefix[0] != 0x04.toByte()) {
            throw IllegalStateException("Wrong uncompressed public key")
        }

        val xyPublicKeyBytes = uncompressedPublicKeyWithPrefix.copyOfRange(1, 65)
        val keccakDigest = Keccak.Digest256() // Keccak-256 из Bouncy Castle
        val hashedPublicKeyBytes = keccakDigest.digest(xyPublicKeyBytes)

        val addressBytes = hashedPublicKeyBytes.takeLast(20).toByteArray()
        return AddressBytesWithPublicKey(addressBytes, uncompressedPublicKeyWithPrefix)
    }
}