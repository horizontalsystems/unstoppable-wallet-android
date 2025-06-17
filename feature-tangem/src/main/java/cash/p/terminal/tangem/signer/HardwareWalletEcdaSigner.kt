package cash.p.terminal.tangem.signer

import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import cash.p.terminal.strings.R
import cash.p.terminal.tangem.domain.TangemConfig
import cash.p.terminal.tangem.domain.canonicalise
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
import io.horizontalsystems.bitcoincore.transactions.builder.IEcdsaInputBatchSigner
import io.horizontalsystems.bitcoincore.transactions.builder.IInputSigner
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.model.DataToSign
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.Utils
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.hdwalletkit.ECDSASignature
import kotlinx.coroutines.delay
import org.koin.java.KoinJavaComponent.inject
import java.math.BigInteger

/***
 * @param onBatchSubscribeAmountReceived to show warning notification how many time to use card for signing
 */
class HardwareWalletEcdaSigner(
    private val hardwarePublicKey: HardwarePublicKey,
    private val cardId: String
) : IInputSigner, IEcdsaInputBatchSigner {

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

    override suspend fun sigScriptEcdsaData(
        transaction: Transaction,
        inputsToSign: List<InputToSign>,
        outputs: List<TransactionOutput>,
        index: Int
    ): List<ByteArray> {
        val transactionSerializer =
            requireNotNull(transactionSerializer) { "Transaction serializer must be set before signing" }
        val network = requireNotNull(network) { "Network must be set before signing" }

        val input = inputsToSign[index]
        val prevOutput = input.previousOutput
        val publicKey = input.previousOutputPublicKey

        val txContent = transactionSerializer.serializeForSignature(
            transaction = transaction,
            inputsToSign = inputsToSign,
            outputs = outputs,
            inputIndex = index,
            isWitness = prevOutput.scriptType.isWitness || network.sigHashForked
        ) + byteArrayOf(network.sigHashValue, 0, 0, 0)

        val hashToSign = Utils.doubleDigest(txContent)
        val changeSegment = if (publicKey.external) "0" else "1"
        val addressIndexSegment = publicKey.index.toString()
        val fullDerivationPathString =
            "${hardwarePublicKey.derivationPath}/$changeSegment/$addressIndexSegment"
        Log.d("HardwareWalletSigner", "sigScriptEcdsaData $fullDerivationPathString")
        val signResponse: CompletionResult<SignResponse> = signHashesTransactionUseCase(
            cardId = cardId,
            hashes = arrayOf(hashToSign),
            walletPublicKey = hardwarePublicKey.publicKey,
            derivationPath = DerivationPath(fullDerivationPathString)
        )
        when (signResponse) {
            is CompletionResult.Success -> {
                val rawSignatureFromTangem = signResponse.data.signatures.firstOrNull()
                    ?: throw Error("No signature returned from signing operation")
                val rBytes = rawSignatureFromTangem.copyOfRange(0, 32)
                val sBytes = rawSignatureFromTangem.copyOfRange(32, 64)
                val r = BigInteger(1, rBytes)
                var s = BigInteger(1, sBytes)
                val derSignatureFromTangem = ECDSASignature(r, s).canonicalise().encodeToDER()
                val finalSignature = derSignatureFromTangem + network.sigHashValue
                return when (prevOutput.scriptType) {
                    ScriptType.P2PK -> listOf(finalSignature)
                    else -> listOf(finalSignature, publicKey.publicKey)
                }
            }

            is CompletionResult.Failure -> throw signResponse.error
        }
    }

    override suspend fun prepareDataForEcdsaSigning(mutableTransaction: MutableTransaction): List<DataToSign> {
        Log.d(
            "HardwareWalletSigner",
            "prepareDataForEcdsaSigning ${mutableTransaction.inputsToSign.size}"
        )
        val transactionSerializer =
            requireNotNull(transactionSerializer) { "Transaction serializer must be set before signing" }
        val network = requireNotNull(network) { "Network must be set before signing" }

        return buildList {
            mutableTransaction.inputsToSign.forEachIndexed { index, input ->
                val prevOutput = input.previousOutput
                val publicKey = input.previousOutputPublicKey

                val txContent = transactionSerializer.serializeForSignature(
                    transaction = mutableTransaction.transaction,
                    inputsToSign = mutableTransaction.inputsToSign,
                    outputs = mutableTransaction.outputs,
                    inputIndex = index,
                    isWitness = prevOutput.scriptType.isWitness || network.sigHashForked
                ) + byteArrayOf(network.sigHashValue, 0, 0, 0)

                add(
                    DataToSign(
                        publicKey = publicKey,
                        scriptType = prevOutput.scriptType,
                        data = Utils.doubleDigest(txContent)
                    )
                )
            }
        }.also {
            updateFlowGroupsToSign(it)
        }
    }

    private fun updateFlowGroupsToSign(data: List<DataToSign>) {
        val groupedData = data.groupBy { it.publicKey to it.scriptType }
        if (groupedData.size > 1) {
            ContextCompat.getMainExecutor(CoreApp.instance).execute {
                Toast.makeText(
                    CoreApp.instance,
                    CoreApp.instance.getString(R.string.need_to_sign_times, groupedData.size),
                    Toast.LENGTH_LONG
                ).show()
            }

        }
    }

    override suspend fun sigScriptEcdsaData(data: List<DataToSign>): List<List<ByteArray>> {
        Log.d("HardwareWalletSigner", "sigScriptEcdsaData ${data.size}")
        val network = requireNotNull(network) { "Network must be set before signing" }
        if (data.isEmpty()) {
            Log.w("HardwareWalletEcdaSigner", "No data to sign")
            return emptyList()
        }

        // Use map to restore original order of data after
        val signedData = mutableMapOf<DataToSign, List<ByteArray>>()
        val groupedData = data.groupBy { it.publicKey to it.scriptType }
        Log.d("HardwareWalletSigner", "sigScriptEcdsaData, groups: ${groupedData.size}")
        groupedData.forEach { (publicKeyAndScriptType, groupForSign) ->
            val publicKey = publicKeyAndScriptType.first
            val scriptType = publicKeyAndScriptType.second

            val dataHashes = groupForSign.map { it.data }
            val changeSegment = if (publicKey.external) "0" else "1"
            val addressIndexSegment = publicKey.index.toString()
            val fullDerivationPathString =
                "${hardwarePublicKey.derivationPath}/$changeSegment/$addressIndexSegment"
            val fullDerivationPathForAddress = DerivationPath(fullDerivationPathString)

            val signResponse: CompletionResult<SignResponse> = signHashesTransactionUseCase(
                cardId = cardId,
                hashes = dataHashes.toTypedArray(),
                walletPublicKey = hardwarePublicKey.publicKey,
                derivationPath = fullDerivationPathForAddress
            )
            when (signResponse) {
                is CompletionResult.Success -> {
                    signResponse.data.signatures.forEachIndexed { index, rawSignatureFromTangem ->
                        val rBytes = rawSignatureFromTangem.copyOfRange(0, 32)
                        val sBytes = rawSignatureFromTangem.copyOfRange(32, 64)
                        val r = BigInteger(1, rBytes)
                        var s = BigInteger(1, sBytes)
                        val derSignatureFromTangem =
                            ECDSASignature(r, s).canonicalise().encodeToDER()
                        val finalSignature = derSignatureFromTangem + network.sigHashValue
                        signedData[groupForSign[index]] = when (scriptType) {
                            ScriptType.P2PK -> listOf(finalSignature)
                            else -> listOf(finalSignature, publicKey.publicKey)
                        }
                    }
                }

                is CompletionResult.Failure -> throw signResponse.error
            }
            if (signedData.size != data.size) {
                delay(TangemConfig.SCAN_DELAY) //delay for tangem sdk not to get Busy error
            }
        }
        return data.map { dataToSign ->
            signedData[dataToSign] ?: throw Error("Signature missing")
        }
    }
}
