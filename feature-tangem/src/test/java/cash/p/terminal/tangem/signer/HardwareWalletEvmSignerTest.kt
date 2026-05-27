package cash.p.terminal.tangem.signer

import cash.p.terminal.tangem.domain.usecase.SignOneHashTransactionUseCase
import cash.p.terminal.wallet.entities.HardwarePublicKey
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.operations.sign.SignHashResponse
import io.horizontalsystems.ethereumkit.crypto.InternalBouncyCastleProvider
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.math.BigInteger
import java.security.Security

class HardwareWalletEvmSignerTest {

    private val signOneHashTransactionUseCase: SignOneHashTransactionUseCase = mockk()

    @Before
    fun setUp() {
        Security.addProvider(InternalBouncyCastleProvider.getInstance())
        startKoin {
            modules(module { single { signOneHashTransactionUseCase } })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    // Real capture from a Trezor Safe 5 BSC token approve (legacy tx); reused here only as a known
    // (hash, signature, public key) triple. On BSC the recovery id is 1.
    private val legacyTx = RawTransaction(
        gasPrice = GasPrice.Legacy(50_000_000L),
        gasLimit = 413779L,
        to = Address(APPROVE_TO),
        value = BigInteger.ZERO,
        nonce = 267L,
        data = APPROVE_DATA.hexToBytes()
    )
    private val legacySignatureBytes =
        ("d752d2389177121be79640f794bcbe8036e14cdfc7a2f4a0a11bbfece058b722" +
            "7a5d0c972c4e44f9d1586cbd629d5db87182554449de9414e4d9eb27b32c72fe").hexToBytes()
    private val legacyPubKey =
        ("04616e4bf0811e707552a8e8e022c0bdbbd30bb46775ac09cf649296186408473f" +
            "bef6b86f23610144aded5ae99193f66ab4709fc4feb844eb80ff5401c4f2aaf8").hexToBytes()
    private val legacySender = "0x24b1bf74dc962ff109048c2e540864324fe6622d"

    // Deterministic EIP-1559 BSC vector (EIP-155 test key 0x4646...46). Recovery id is 0.
    private val eip1559Tx = RawTransaction(
        gasPrice = GasPrice.Eip1559(maxFeePerGas = 3_000_000_000L, maxPriorityFeePerGas = 1_000_000_000L),
        gasLimit = 250_000L,
        to = Address(APPROVE_TO),
        value = BigInteger.ZERO,
        nonce = 5L,
        data = APPROVE_DATA.hexToBytes()
    )
    private val eip1559SignatureBytes =
        ("101fdd98aa2871e985499dedc4de19810db6cbcf6fcb376fedf4cb56cf0277c5" +
            "711751862e80fcf8e1e2dd0d387e8d0248738e6a9a92357a8f5d035f0c820c60").hexToBytes()
    private val eip1559PubKey =
        ("044bc2a31265153f07e70e0bab08724e6b85e217f8cd628ceb62974247bb493382" +
            "ce28cab79ad7119ee1ad3ebcdb98a16805211530ecc6cfefa1b88e6dff99232a").hexToBytes()
    private val eip1559Sender = "0x9d8a62f656a8d1615c1294fd71e9cfb3e4855a4f"

    @Test
    fun signature_legacyTransactionOnBsc_buildsEip155V() = runBlocking {
        stubSuccess(legacySignatureBytes)

        val signature = createSigner(legacySender, legacyPubKey).signature(legacyTx)

        // v = recId(1) + 35 + 2 * chainId(56) = 148
        assertEquals(148, signature.v)
        assertArrayEquals(legacySignatureBytes.sliceArray(0..31), signature.r)
        assertArrayEquals(legacySignatureBytes.sliceArray(32..63), signature.s)
    }

    @Test
    fun signature_eip1559Transaction_usesRecoveryIdAsV() = runBlocking {
        stubSuccess(eip1559SignatureBytes)

        val signature = createSigner(eip1559Sender, eip1559PubKey).signature(eip1559Tx)

        // EIP-1559 yParity equals the recovery id (0 here).
        assertEquals(0, signature.v)
        assertArrayEquals(eip1559SignatureBytes.sliceArray(0..31), signature.r)
        assertArrayEquals(eip1559SignatureBytes.sliceArray(32..63), signature.s)
    }

    @Test
    fun signature_wrongSignatureSize_throws() {
        stubSuccess(ByteArray(63))

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { createSigner(legacySender, legacyPubKey).signature(legacyTx) }
        }
    }

    @Test
    fun signature_userCancelled_propagatesTangemError() {
        coEvery {
            signOneHashTransactionUseCase(any(), any(), any(), any())
        } returns CompletionResult.Failure(TangemSdkError.UserCancelled())

        assertThrows(TangemSdkError.UserCancelled::class.java) {
            runBlocking { createSigner(legacySender, legacyPubKey).signature(legacyTx) }
        }
    }

    private fun stubSuccess(signatureBytes: ByteArray) {
        coEvery { signOneHashTransactionUseCase(any(), any(), any(), any()) } returns
            CompletionResult.Success(
                SignHashResponse(
                    cardId = "card",
                    walletPublicKey = ByteArray(0),
                    signature = signatureBytes,
                    totalSignedHashes = 1
                )
            )
    }

    private fun createSigner(address: String, expectedPublicKeyBytes: ByteArray): HardwareWalletEvmSigner {
        val hardwarePublicKey = mockk<HardwarePublicKey> {
            every { publicKey } returns expectedPublicKeyBytes
            every { derivationPath } returns "m/44'/60'/0'/0/0"
        }
        return HardwareWalletEvmSigner(
            address = Address(address),
            publicKey = hardwarePublicKey,
            chain = Chain.BinanceSmartChain,
            expectedPublicKeyBytes = expectedPublicKeyBytes
        )
    }

    private fun String.hexToBytes(): ByteArray =
        removePrefix("0x").chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    companion object {
        private const val APPROVE_TO = "0xba2ae424d960c26247dd6c32edc70b295c744c43"
        private const val APPROVE_DATA =
            "0x095ea7b300000000000000000000000013f4ea83d0bd40e75c8222255bc855a974568dd4" +
                "000000000000000000000000000000000000000000000000000000003b034361"
    }
}
