package cash.p.terminal.core.utils

import io.horizontalsystems.hdwalletkit.Base58
import io.horizontalsystems.tronkit.models.Address
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.math.ec.ECCurve
import org.bouncycastle.math.ec.ECPoint
import java.security.MessageDigest

object TronAddressParser {

    private const val XPUB_EXPECTED_DECODED_LENGTH_WITH_CHECKSUM = 82
    private const val XPUB_PAYLOAD_LENGTH = 78
    private const val TRON_ADDRESS_PREFIX_BYTE: Byte = 0x41.toByte()

    fun parseXpubToTronAddress(xPubKey: String): TronAddressAndPublicKey {
        val decodedXpubWithChecksum = Base58.decode(xPubKey)
        if (decodedXpubWithChecksum.size != XPUB_EXPECTED_DECODED_LENGTH_WITH_CHECKSUM) {
            throw IllegalArgumentException(
                "Invalid decoded xPub length. Expected ${XPUB_EXPECTED_DECODED_LENGTH_WITH_CHECKSUM}, got ${decodedXpubWithChecksum.size}"
            )
        }

        val xpubPayload = decodedXpubWithChecksum.copyOfRange(0, XPUB_PAYLOAD_LENGTH)
        val receivedChecksum = decodedXpubWithChecksum.copyOfRange(
            XPUB_PAYLOAD_LENGTH,
            XPUB_EXPECTED_DECODED_LENGTH_WITH_CHECKSUM
        )

        val sha256 = MessageDigest.getInstance("SHA-256")
        val calculatedXpubChecksum = sha256.digest(sha256.digest(xpubPayload)).copyOfRange(0, 4)

        if (!calculatedXpubChecksum.contentEquals(receivedChecksum)) {
            throw IllegalArgumentException("Invalid xPub checksum.")
        }

        val compressedPublicKeyBytes = xpubPayload.copyOfRange(45, XPUB_PAYLOAD_LENGTH)

        val ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val curve: ECCurve = ecSpec.curve
        val ecPoint: ECPoint = curve.decodePoint(compressedPublicKeyBytes)

        val uncompressedPublicKeyWithPrefix = ecPoint.getEncoded(false)
        if (uncompressedPublicKeyWithPrefix.size != 65 || uncompressedPublicKeyWithPrefix[0] != 0x04.toByte()) {
            throw IllegalStateException("Wrong uncompressed public key format")
        }
        val xyPublicKeyBytes = uncompressedPublicKeyWithPrefix.copyOfRange(1, 65)

        val keccakDigest = Keccak.Digest256()
        val hashedPublicKeyBytes = keccakDigest.digest(xyPublicKeyBytes)

        val addressCoreBytes = hashedPublicKeyBytes.takeLast(20).toByteArray()

        val addressWithPrefix = byteArrayOf(TRON_ADDRESS_PREFIX_BYTE) + addressCoreBytes

        val tronAddrChecksum = sha256.digest(sha256.digest(addressWithPrefix)).copyOfRange(0, 4)

        val dataToEncodeInBase58 = addressWithPrefix + tronAddrChecksum

        return TronAddressAndPublicKey(
            Address.fromBase58(Base58.encode(dataToEncodeInBase58)),
            uncompressedPublicKeyWithPrefix
        )
    }
}

class TronAddressAndPublicKey(
    val address: Address,
    val publicKey: ByteArray
)