package cash.p.terminal.tangem.signer

import android.util.Log
import cash.p.terminal.tangem.domain.usecase.SignHashesTransactionUseCase
import cash.p.terminal.wallet.entities.HardwarePublicKey
import com.tangem.common.CompletionResult
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.operations.sign.SignResponse
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.serializers.BaseTransactionSerializer
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.bitcoincore.transactions.builder.ISchnorrInputBatchSigner
import io.horizontalsystems.bitcoincore.transactions.builder.ISchnorrInputSigner
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.model.DataToSign
import org.koin.java.KoinJavaComponent.inject


class HardwareWalletSchnorrSigner(
    private val hardwarePublicKey: HardwarePublicKey,
    private val cardId: String,
) : ISchnorrInputSigner, ISchnorrInputBatchSigner {

    private val signHashesTransactionUseCase: SignHashesTransactionUseCase by inject(
        SignHashesTransactionUseCase::class.java
    )

    private var transactionSerializer: BaseTransactionSerializer? = null
    private var network: Network? = null

    override fun setTransactionSerializer(serializer: BaseTransactionSerializer) {
        this.transactionSerializer = serializer
    }

    override fun setNetwork(network: Network) {
        this.network = network
    }

    override suspend fun prepareDataForSchnorrSigning(mutableTransaction: MutableTransaction): List<DataToSign> {
        Log.d("HardwareWalletSigner", "prepareDataForSchnorrSigning ${mutableTransaction.inputsToSign.size}")
        return buildList {
            mutableTransaction.inputsToSign.forEachIndexed { index, input ->
                val transactionSerializer =
                    requireNotNull(transactionSerializer) { "Transaction serializer must be set before signing" }
                val publicKey = input.previousOutputPublicKey
                val serializedTransaction = transactionSerializer.serializeForTaprootSignature(
                    mutableTransaction.transaction,
                    mutableTransaction.inputsToSign,
                    mutableTransaction.outputs,
                    index
                )

                add(
                    DataToSign(
                        publicKey = publicKey,
                        scriptType = input.previousOutput.scriptType,
                        data = io.horizontalsystems.hdwalletkit.Utils.taggedHash(
                            "TapSighash",
                            serializedTransaction
                        )
                    )

                )
            }
        }
    }

    override suspend fun sigScriptSchnorrData(data: List<DataToSign>): List<ByteArray> {
        if (data.isEmpty()) {
            Log.w("HardwareWalletEcdaSigner", "No data to sign")
            return emptyList()
        }

        val dataToSign = data.map { it.data }

        val publicKey = data.first().publicKey

        val changeSegment = if (publicKey.external) "0" else "1"
        val addressIndexSegment = publicKey.index.toString()
        val fullDerivationPathString =
            "${hardwarePublicKey.derivationPath}/$changeSegment/$addressIndexSegment"
        Log.d("HardwareWalletSigner", "sigScriptSchnorrData $fullDerivationPathString")
        val walletPublicKey =
            hardwarePublicKey.publicKey.copyOfRange(1, hardwarePublicKey.publicKey.size)
        val signResponse: CompletionResult<SignResponse> = signHashesTransactionUseCase(
            cardId = cardId,
            hashes = dataToSign.toTypedArray(),
            walletPublicKey = walletPublicKey,
            derivationPath = DerivationPath(fullDerivationPathString)
        )
        when (signResponse) {
            is CompletionResult.Success -> {
                return signResponse.data.signatures
            }

            is CompletionResult.Failure -> throw signResponse.error
        }

    }

    override suspend fun sigScriptSchnorrData(
        transaction: Transaction,
        inputsToSign: List<InputToSign>,
        outputs: List<TransactionOutput>,
        index: Int
    ): List<ByteArray> {
        val transactionSerializer =
            requireNotNull(transactionSerializer) { "Transaction serializer must be set before signing" }
        val input = inputsToSign[index]
        val publicKey = input.previousOutputPublicKey
        val serializedTransaction = transactionSerializer.serializeForTaprootSignature(
            transaction,
            inputsToSign,
            outputs,
            index
        )

        val signatureHash =
            io.horizontalsystems.hdwalletkit.Utils.taggedHash("TapSighash", serializedTransaction)
        val changeSegment = if (publicKey.external) "0" else "1"
        val addressIndexSegment = publicKey.index.toString()
        val fullDerivationPathString =
            "${hardwarePublicKey.derivationPath}/$changeSegment/$addressIndexSegment"
        Log.d("HardwareWalletSigner", "sigScriptSchnorrData $fullDerivationPathString")
        val walletPublicKey =
            hardwarePublicKey.publicKey.copyOfRange(1, hardwarePublicKey.publicKey.size)
        val signResponse: CompletionResult<SignResponse> = signHashesTransactionUseCase(
            cardId = cardId,
            hashes = arrayOf(signatureHash),
            walletPublicKey = walletPublicKey,
            derivationPath = DerivationPath(fullDerivationPathString)
        )
        when (signResponse) {
            is CompletionResult.Success -> {
                val rawSignatureFromTangem = signResponse.data.signatures.firstOrNull()
                    ?: throw Error("No signature returned from signing operation")
                if (rawSignatureFromTangem.size != 64) {
                    throw Error("Invalid Schnorr signature length: ${rawSignatureFromTangem.size}")
                }
                return listOf(rawSignatureFromTangem)
            }

            is CompletionResult.Failure -> throw signResponse.error
        }
    }
}
