package cash.p.terminal.trezor.signer

import cash.p.terminal.trezor.domain.TrezorDeepLinkManager
import cash.p.terminal.trezor.domain.TrezorSigningException
import cash.p.terminal.trezor.domain.hexBytes
import cash.p.terminal.trezor.domain.hexInt
import cash.p.terminal.trezor.domain.model.TrezorMethod
import cash.p.terminal.trezor.domain.requirePayload
import cash.p.terminal.wallet.crypto.EvmSignatureRecovery
import io.horizontalsystems.ethereumkit.core.TransactionBuilder
import io.horizontalsystems.ethereumkit.core.TransactionSigner
import io.horizontalsystems.ethereumkit.core.signer.EthSigner
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.crypto.EIP712Encoder
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.horizontalsystems.ethereumkit.models.Signature
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import io.horizontalsystems.ethereumkit.spv.rlp.RLPList
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigInteger

class TrezorEvmSigner(
    private val address: Address,
    private val chain: Chain,
    private val derivationPath: String,
    private val deepLinkManager: TrezorDeepLinkManager
) : Signer(
    transactionBuilder = TransactionBuilder(address, chain.id),
    transactionSigner = TransactionSigner(MOCK_PRIVATE_KEY, chain.id),
    ethSigner = EthSigner(MOCK_PRIVATE_KEY, CryptoUtils, EIP712Encoder())
) {
    companion object {
        private val MOCK_PRIVATE_KEY = BigInteger.ONE

        private const val LEGACY_GAS_PRICE_INDEX = 1
        private const val LEGACY_GAS_LIMIT_INDEX = 2
        private const val EIP1559_MAX_PRIORITY_FEE_INDEX = 2
        private const val EIP1559_MAX_FEE_INDEX = 3
        private const val EIP1559_GAS_LIMIT_INDEX = 4
    }

    /**
     * Not supported for Trezor: the base [Signer] contract returns only a [Signature], but Trezor
     * Suite re-estimates gas fields and signs a transaction that differs from the one we requested.
     * A bare signature would be broadcast against our stale raw transaction and recover a wrong
     * sender. Use [signTransaction], which returns the transaction the device actually signed.
     */
    override suspend fun signature(rawTransaction: RawTransaction): Signature =
        throw TrezorSigningException("Use signTransaction() for Trezor; signature() loses the device-signed fields")

    /**
     * Signs the transaction on the device and returns both the signature and the transaction the
     * device actually signed. Trezor Suite re-estimates the gas fields instead of honoring the ones
     * we send, so the device signs over different values than requested. Broadcasting our original
     * raw transaction with that signature would make the node recover a wrong sender ("balance 0"),
     * so we rebuild the raw transaction from the gas fields the device signed.
     */
    suspend fun signTransaction(rawTransaction: RawTransaction): SignedEvmTransaction {
        val params = buildJsonObject {
            put("path", derivationPath)
            put("transaction", buildTransactionJson(rawTransaction))
        }
        val payload = deepLinkManager.call(TrezorMethod.EthSignTransaction, params).requirePayload()
        val signature = Signature(
            v = payload.hexInt("v"),
            r = payload.hexBytes("r"),
            s = payload.hexBytes("s")
        )
        if (!payload.containsKey("serializedTx")) {
            throw TrezorSigningException("Trezor response has no serializedTx")
        }
        val serializedTx = payload.hexBytes("serializedTx")
        val signedRawTransaction = reconcileSignedTransaction(rawTransaction, signature, serializedTx)
        verifySender(signedRawTransaction, signature)
        return SignedEvmTransaction(signature, signedRawTransaction)
    }

    private fun reconcileSignedTransaction(
        rawTransaction: RawTransaction,
        signature: Signature,
        serializedTx: ByteArray
    ): RawTransaction {
        val (signedGasPrice, signedGasLimit) = decodeSignedGas(serializedTx, rawTransaction.gasPrice)
        val signedRawTransaction = RawTransaction(
            gasPrice = signedGasPrice,
            gasLimit = signedGasLimit,
            to = rawTransaction.to,
            value = rawTransaction.value,
            nonce = rawTransaction.nonce,
            data = rawTransaction.data
        )
        if (!TransactionBuilder.encode(signedRawTransaction, signature, chain.id).contentEquals(serializedTx)) {
            throw TrezorSigningException("Rebuilt transaction does not match the device signature")
        }
        return signedRawTransaction
    }

    /**
     * Defense in depth on top of the byte-equality check: recovers the sender from the device
     * signature and confirms it matches our wallet address, so a signature from the wrong account
     * can never be broadcast.
     */
    private fun verifySender(signedRawTransaction: RawTransaction, signature: Signature) {
        val recovered = EvmSignatureRecovery.recoverSenderAddress(signedRawTransaction, signature, chain.id)
            ?: throw TrezorSigningException("Cannot recover sender from the device signature")
        if (recovered != address) {
            throw TrezorSigningException("Device signed with an unexpected account")
        }
    }

    private fun decodeSignedGas(serializedTx: ByteArray, requestedGasPrice: GasPrice): Pair<GasPrice, Long> {
        val (rlpPayload, isLegacy) = when (requestedGasPrice) {
            is GasPrice.Legacy -> serializedTx to true
            is GasPrice.Eip1559 -> serializedTx.copyOfRange(1, serializedTx.size) to false
        }
        val fields = RLP.decode2(rlpPayload).firstOrNull() as? RLPList
            ?: throw TrezorSigningException("Malformed serialized transaction")
        return if (isLegacy) {
            GasPrice.Legacy(fields.longAt(LEGACY_GAS_PRICE_INDEX)) to fields.longAt(LEGACY_GAS_LIMIT_INDEX)
        } else {
            val gasPrice = GasPrice.Eip1559(
                maxFeePerGas = fields.longAt(EIP1559_MAX_FEE_INDEX),
                maxPriorityFeePerGas = fields.longAt(EIP1559_MAX_PRIORITY_FEE_INDEX)
            )
            gasPrice to fields.longAt(EIP1559_GAS_LIMIT_INDEX)
        }
    }

    private fun RLPList.longAt(index: Int): Long =
        BigInteger(1, this[index].rlpData ?: ByteArray(0)).toLong()

    private fun buildTransactionJson(rawTransaction: RawTransaction) = buildJsonObject {
        put("to", rawTransaction.to.hex)
        put("value", "0x" + rawTransaction.value.toString(16))
        put("gasLimit", "0x" + rawTransaction.gasLimit.toString(16))
        put("nonce", "0x" + rawTransaction.nonce.toString(16))
        put("data", rawTransaction.data.toHexString())
        put("chainId", chain.id)
        when (val gp = rawTransaction.gasPrice) {
            is GasPrice.Legacy -> {
                put("gasPrice", "0x" + gp.legacyGasPrice.toString(16))
            }
            is GasPrice.Eip1559 -> {
                put("maxFeePerGas", "0x" + gp.maxFeePerGas.toString(16))
                put("maxPriorityFeePerGas", "0x" + gp.maxPriorityFeePerGas.toString(16))
            }
        }
    }
}

data class SignedEvmTransaction(
    val signature: Signature,
    val rawTransaction: RawTransaction
)
