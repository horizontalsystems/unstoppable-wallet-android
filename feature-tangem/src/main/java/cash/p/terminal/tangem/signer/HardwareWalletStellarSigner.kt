package cash.p.terminal.tangem.signer

import cash.p.terminal.tangem.domain.usecase.SignOneHashTransactionUseCase
import cash.p.terminal.wallet.entities.HardwarePublicKey
import com.tangem.common.CompletionResult
import com.tangem.crypto.hdWallet.DerivationPath
import io.horizontalsystems.stellarkit.Signer
import org.koin.java.KoinJavaComponent.inject
import org.stellar.sdk.xdr.DecoratedSignature
import org.stellar.sdk.xdr.Signature
import org.stellar.sdk.xdr.SignatureHint

class HardwareWalletStellarSigner(private val hardwarePublicKey: HardwarePublicKey) : Signer {

    private val signOneHashTransactionUseCase: SignOneHashTransactionUseCase by inject(
        SignOneHashTransactionUseCase::class.java
    )
    override val publicKey: ByteArray = hardwarePublicKey.derivedPublicKey

    override fun canSign() = true

    override suspend fun sign(hash: ByteArray): DecoratedSignature {
        val signBytesResponse =
            signOneHashTransactionUseCase(
                hash = hash,
                walletPublicKey = hardwarePublicKey.publicKey,
                derivationPath = DerivationPath(hardwarePublicKey.derivationPath)
            )
        when (signBytesResponse) {
            is CompletionResult.Success -> {
                val signature = Signature()
                signature.signature = signBytesResponse.data.signature

                val signatureHintBytes: ByteArray =
                    publicKey.copyOfRange(publicKey.size - 4, publicKey.size)
                val signatureHint = SignatureHint()
                signatureHint.signatureHint = signatureHintBytes

                val decoratedSignature = DecoratedSignature()
                decoratedSignature.hint = signatureHint
                decoratedSignature.signature = signature
                return decoratedSignature
            }

            is CompletionResult.Failure -> {
                throw Exception("Signing failed: ${signBytesResponse.error}")
            }
        }
    }
}