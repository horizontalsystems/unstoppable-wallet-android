package cash.p.terminal.wallet.crypto

import io.horizontalsystems.ethereumkit.crypto.InternalBouncyCastleProvider
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.horizontalsystems.ethereumkit.models.Signature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.math.BigInteger
import java.security.Security

class EvmSignatureRecoveryTest {

    @Before
    fun setUp() {
        Security.addProvider(InternalBouncyCastleProvider.getInstance())
    }

    private val bscChainId = Chain.BSC

    // Real capture from a Trezor Safe 5 BSC token approve (legacy tx).
    private val legacyTx = RawTransaction(
        gasPrice = GasPrice.Legacy(50_000_000L),
        gasLimit = 413779L,
        to = Address(APPROVE_TO),
        value = BigInteger.ZERO,
        nonce = 267L,
        data = APPROVE_DATA.hexToBytes()
    )
    private val legacySignature = Signature(
        v = 148,
        r = "0xd752d2389177121be79640f794bcbe8036e14cdfc7a2f4a0a11bbfece058b722".hexToBytes(),
        s = "0x7a5d0c972c4e44f9d1586cbd629d5db87182554449de9414e4d9eb27b32c72fe".hexToBytes()
    )
    private val legacySender = Address("0x24b1bf74dc962ff109048c2e540864324fe6622d")
    // Uncompressed (0x04-prefixed) public key of the legacy sender.
    private val legacyPubKey =
        ("0x04616e4bf0811e707552a8e8e022c0bdbbd30bb46775ac09cf649296186408473f" +
            "bef6b86f23610144aded5ae99193f66ab4709fc4feb844eb80ff5401c4f2aaf8").hexToBytes()

    // Deterministic EIP-1559 BSC vector (private key 0x4646...46).
    private val eip1559Tx = RawTransaction(
        gasPrice = GasPrice.Eip1559(maxFeePerGas = 3_000_000_000L, maxPriorityFeePerGas = 1_000_000_000L),
        gasLimit = 250_000L,
        to = Address(APPROVE_TO),
        value = BigInteger.ZERO,
        nonce = 5L,
        data = APPROVE_DATA.hexToBytes()
    )
    private val eip1559Signature = Signature(
        v = 0,
        r = "0x101fdd98aa2871e985499dedc4de19810db6cbcf6fcb376fedf4cb56cf0277c5".hexToBytes(),
        s = "0x711751862e80fcf8e1e2dd0d387e8d0248738e6a9a92357a8f5d035f0c820c60".hexToBytes()
    )
    private val eip1559Sender = Address("0x9d8a62f656a8d1615c1294fd71e9cfb3e4855a4f")

    @Test
    fun signingHash_legacyTransaction_matchesGroundTruth() {
        assertEquals(
            "60bbf99321a309d8b892d983ce82063c21dfd7826752110b52e4f64cbfb99bfa",
            EvmSignatureRecovery.signingHash(legacyTx, bscChainId).toHex()
        )
    }

    @Test
    fun signingHash_eip1559Transaction_matchesGroundTruth() {
        assertEquals(
            "0cc831ad523cb7a1080f80bd9ce7c86ba8673dce3f613ab8752f1fc7b5f3624f",
            EvmSignatureRecovery.signingHash(eip1559Tx, bscChainId).toHex()
        )
    }

    @Test
    fun recoverSenderAddress_legacyTransaction_recoversSender() {
        assertEquals(legacySender, EvmSignatureRecovery.recoverSenderAddress(legacyTx, legacySignature, bscChainId))
    }

    @Test
    fun recoverSenderAddress_eip1559Transaction_recoversSender() {
        assertEquals(eip1559Sender, EvmSignatureRecovery.recoverSenderAddress(eip1559Tx, eip1559Signature, bscChainId))
    }

    @Test
    fun recoverSenderAddress_legacyWrongChainId_returnsNullOrDifferentSender() {
        // On a wrong chainId the EIP-155 recovery id no longer reduces to 0/1.
        assertNull(EvmSignatureRecovery.recoverSenderAddress(legacyTx, legacySignature, chainId = 1))
    }

    @Test
    fun findRecoveryId_legacySignature_findsRecId1() {
        val recId = EvmSignatureRecovery.findRecoveryId(
            messageHash = EvmSignatureRecovery.signingHash(legacyTx, bscChainId),
            r = BigInteger(1, legacySignature.r),
            s = BigInteger(1, legacySignature.s),
            expectedPublicKeyBytes = legacyPubKey
        )
        assertEquals(1, recId)
    }

    private object Chain {
        const val BSC = 56
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray =
        removePrefix("0x").chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    companion object {
        private const val APPROVE_TO = "0xba2ae424d960c26247dd6c32edc70b295c744c43"
        private const val APPROVE_DATA =
            "0x095ea7b300000000000000000000000013f4ea83d0bd40e75c8222255bc855a974568dd4" +
                "000000000000000000000000000000000000000000000000000000003b034361"
    }
}
