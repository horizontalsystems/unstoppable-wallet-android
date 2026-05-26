package cash.p.terminal.trezor.signer

import cash.p.terminal.trezor.domain.TrezorDeepLinkManager
import cash.p.terminal.trezor.domain.TrezorSigningException
import cash.p.terminal.trezor.domain.model.TrezorMethod
import cash.p.terminal.trezor.domain.model.TrezorResponse
import io.horizontalsystems.ethereumkit.crypto.InternalBouncyCastleProvider
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.math.BigInteger
import java.security.Security

class TrezorEvmSignerTest {

    private val deepLinkManager: TrezorDeepLinkManager = mockk()

    @Before
    fun setUp() {
        Security.addProvider(InternalBouncyCastleProvider.getInstance())
    }

    // Real capture from a Trezor Safe 5 BSC token approve (legacy tx). The app requested gasLimit
    // 0xe8f0 (59632) but Trezor Suite re-estimated and signed over gasLimit 0x065053 (413779).
    private val toAddress = "0xba2ae424d960c26247dd6c32edc70b295c744c43"
    private val data = "0x095ea7b300000000000000000000000013f4ea83d0bd40e75c8222255bc855a974568dd4" +
        "000000000000000000000000000000000000000000000000000000003b034361"
    private val requestedGasLimit = 59632L
    private val signedGasLimit = 413779L
    private val gasPrice = 50_000_000L
    private val nonce = 267L

    // The account that actually signed the captured transaction.
    private val legacySender = "0x24b1bf74dc962ff109048c2e540864324fe6622d"

    private val responseV = "0x94"
    private val responseR = "0xd752d2389177121be79640f794bcbe8036e14cdfc7a2f4a0a11bbfece058b722"
    private val responseS = "0x7a5d0c972c4e44f9d1586cbd629d5db87182554449de9414e4d9eb27b32c72fe"
    private val serializedTx = "0xf8ac82010b8402faf0808306505394ba2ae424d960c26247dd6c32edc70b295c744c43" +
        "80b844095ea7b300000000000000000000000013f4ea83d0bd40e75c8222255bc855a974568dd400000000" +
        "0000000000000000000000000000000000000000000000003b0343618194a0d752d2389177121be79640f7" +
        "94bcbe8036e14cdfc7a2f4a0a11bbfece058b722a07a5d0c972c4e44f9d1586cbd629d5db87182554449de9" +
        "414e4d9eb27b32c72fe"

    // Deterministic EIP-1559 BSC vector (private key 0x4646...46). Requested gasLimit differs from
    // the signed one to exercise gas-limit reconciliation on the typed-transaction path too.
    private val eip1559Sender = "0x9d8a62f656a8d1615c1294fd71e9cfb3e4855a4f"
    private val eip1559Nonce = 5L
    private val eip1559MaxFee = 3_000_000_000L
    private val eip1559MaxPriorityFee = 1_000_000_000L
    private val eip1559SignedGasLimit = 250_000L
    private val eip1559ResponseV = "0x0"
    private val eip1559ResponseR = "0x101fdd98aa2871e985499dedc4de19810db6cbcf6fcb376fedf4cb56cf0277c5"
    private val eip1559ResponseS = "0x711751862e80fcf8e1e2dd0d387e8d0248738e6a9a92357a8f5d035f0c820c60"
    private val eip1559SerializedTx = "0x02f8b03805843b9aca0084b2d05e008303d09094ba2ae424d960c26247dd6c32" +
        "edc70b295c744c4380b844095ea7b300000000000000000000000013f4ea83d0bd40e75c8222255bc855a974568dd4" +
        "000000000000000000000000000000000000000000000000000000003b034361c080a0101fdd98aa2871e985499dedc" +
        "4de19810db6cbcf6fcb376fedf4cb56cf0277c5a0711751862e80fcf8e1e2dd0d387e8d0248738e6a9a92357a8f5d03" +
        "5f0c820c60"

    @Test
    fun signTransaction_suiteReestimatesGasLimit_reconcilesToSignedGasLimit() = runBlocking {
        stubResponse(fullPayload())
        val rawTransaction = requestedRawTransaction()

        val signed = createSigner().signTransaction(rawTransaction)

        assertEquals(signedGasLimit, signed.rawTransaction.gasLimit)
        assertEquals(gasPrice, (signed.rawTransaction.gasPrice as GasPrice.Legacy).legacyGasPrice)
        assertEquals(rawTransaction.nonce, signed.rawTransaction.nonce)
        assertEquals(rawTransaction.to, signed.rawTransaction.to)
        assertEquals(rawTransaction.value, signed.rawTransaction.value)
        assertArrayEquals(rawTransaction.data, signed.rawTransaction.data)
    }

    @Test
    fun signTransaction_validResponse_parsesSignature() = runBlocking {
        stubResponse(fullPayload())

        val signed = createSigner().signTransaction(requestedRawTransaction())

        assertEquals(148, signed.signature.v)
        assertArrayEquals(responseR.hexToBytes(), signed.signature.r)
        assertArrayEquals(responseS.hexToBytes(), signed.signature.s)
    }

    @Test
    fun signTransaction_eip1559SuiteReestimatesGasLimit_reconcilesGasFields() = runBlocking {
        stubResponse(eip1559FullPayload())
        val requested = eip1559RequestedRawTransaction()

        val signed = createSigner(eip1559Sender).signTransaction(requested)

        val signedGasPrice = signed.rawTransaction.gasPrice as GasPrice.Eip1559
        assertEquals(eip1559SignedGasLimit, signed.rawTransaction.gasLimit)
        assertEquals(eip1559MaxFee, signedGasPrice.maxFeePerGas)
        assertEquals(eip1559MaxPriorityFee, signedGasPrice.maxPriorityFeePerGas)
        assertEquals(requested.nonce, signed.rawTransaction.nonce)
        assertEquals(requested.to, signed.rawTransaction.to)
        assertEquals(requested.value, signed.rawTransaction.value)
        assertArrayEquals(requested.data, signed.rawTransaction.data)
    }

    @Test
    fun signTransaction_suiteChangesGasPrice_reconcilesToSignedGasPrice() = runBlocking {
        stubResponse(fullPayload())
        // Request a gasPrice that differs from the one the device signed. Reconciliation must adopt
        // the device's value; keeping the requested one would break the byte-equality check.
        val requested = RawTransaction(
            gasPrice = GasPrice.Legacy(gasPrice * 10),
            gasLimit = requestedGasLimit,
            to = Address(toAddress),
            value = BigInteger.ZERO,
            nonce = nonce,
            data = data.hexToBytes()
        )

        val signed = createSigner().signTransaction(requested)

        assertEquals(gasPrice, (signed.rawTransaction.gasPrice as GasPrice.Legacy).legacyGasPrice)
    }

    @Test
    fun signTransaction_eip1559SuiteChangesFees_reconcilesToSignedFees() = runBlocking {
        stubResponse(eip1559FullPayload())
        // Request fee fields that differ from those the device signed; the signed values must win.
        val requested = RawTransaction(
            gasPrice = GasPrice.Eip1559(
                maxFeePerGas = eip1559MaxFee * 2,
                maxPriorityFeePerGas = eip1559MaxPriorityFee * 2
            ),
            gasLimit = requestedGasLimit,
            to = Address(toAddress),
            value = BigInteger.ZERO,
            nonce = eip1559Nonce,
            data = data.hexToBytes()
        )

        val signed = createSigner(eip1559Sender).signTransaction(requested)

        val signedGasPrice = signed.rawTransaction.gasPrice as GasPrice.Eip1559
        assertEquals(eip1559MaxFee, signedGasPrice.maxFeePerGas)
        assertEquals(eip1559MaxPriorityFee, signedGasPrice.maxPriorityFeePerGas)
    }

    @Test
    fun signTransaction_missingSerializedTx_throws() {
        val payload = buildJsonObject {
            put("v", responseV)
            put("r", responseR)
            put("s", responseS)
        }
        stubResponse(payload)

        assertThrows(TrezorSigningException::class.java) {
            runBlocking { createSigner().signTransaction(requestedRawTransaction()) }
        }
    }

    @Test
    fun signTransaction_rebuiltTransactionDiffersFromSignature_throws() {
        stubResponse(fullPayload())
        // The device signed value 0, but here we request a non-zero value. The rebuilt transaction
        // no longer re-encodes to the device's serializedTx, so reconciliation must fail loudly.
        val tampered = RawTransaction(
            gasPrice = GasPrice.Legacy(gasPrice),
            gasLimit = requestedGasLimit,
            to = Address(toAddress),
            value = BigInteger.ONE,
            nonce = nonce,
            data = data.hexToBytes()
        )

        assertThrows(TrezorSigningException::class.java) {
            runBlocking { createSigner().signTransaction(tampered) }
        }
    }

    @Test
    fun signTransaction_deviceSignedWithUnexpectedAccount_throws() {
        stubResponse(fullPayload())
        // The signer is configured with a different wallet address than the one that produced the
        // captured signature, so the sender guard must reject it.
        val wrongAddress = "0x000000000000000000000000000000000000dead"

        assertThrows(TrezorSigningException::class.java) {
            runBlocking { createSigner(wrongAddress).signTransaction(requestedRawTransaction()) }
        }
    }

    @Test
    fun signature_called_throws() {
        assertThrows(TrezorSigningException::class.java) {
            runBlocking { createSigner().signature(requestedRawTransaction()) }
        }
    }

    private fun fullPayload(): JsonObject = buildJsonObject {
        put("v", responseV)
        put("r", responseR)
        put("s", responseS)
        put("serializedTx", serializedTx)
    }

    private fun eip1559FullPayload(): JsonObject = buildJsonObject {
        put("v", eip1559ResponseV)
        put("r", eip1559ResponseR)
        put("s", eip1559ResponseS)
        put("serializedTx", eip1559SerializedTx)
    }

    private fun stubResponse(payload: JsonObject) {
        coEvery { deepLinkManager.call(TrezorMethod.EthSignTransaction, any()) } returns
            TrezorResponse(success = true, payload = payload)
    }

    private fun requestedRawTransaction() = RawTransaction(
        gasPrice = GasPrice.Legacy(gasPrice),
        gasLimit = requestedGasLimit,
        to = Address(toAddress),
        value = BigInteger.ZERO,
        nonce = nonce,
        data = data.hexToBytes()
    )

    private fun eip1559RequestedRawTransaction() = RawTransaction(
        gasPrice = GasPrice.Eip1559(maxFeePerGas = eip1559MaxFee, maxPriorityFeePerGas = eip1559MaxPriorityFee),
        gasLimit = requestedGasLimit,
        to = Address(toAddress),
        value = BigInteger.ZERO,
        nonce = eip1559Nonce,
        data = data.hexToBytes()
    )

    private fun createSigner(address: String = legacySender) = TrezorEvmSigner(
        address = Address(address),
        chain = Chain.BinanceSmartChain,
        derivationPath = "m/44'/60'/0'/0/0",
        deepLinkManager = deepLinkManager
    )

    private fun String.hexToBytes(): ByteArray =
        removePrefix("0x").chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
