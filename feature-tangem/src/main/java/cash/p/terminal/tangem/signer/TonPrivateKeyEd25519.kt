package cash.p.terminal.tangem.signer

import cash.p.terminal.tangem.domain.usecase.SignOneHashTransactionUseCase
import cash.p.terminal.wallet.entities.HardwarePublicKey
import com.tangem.common.CompletionResult
import com.tangem.crypto.hdWallet.DerivationPath
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import org.ton.api.pk.PrivateKeyEd25519
import org.ton.api.pub.PublicKeyEd25519
import org.ton.bitstring.Bits256

class TonPrivateKeyEd25519(
    private val hardwarePublicKey: HardwarePublicKey
) : PrivateKeyEd25519 {
    private val pubKey = PublicKeyEd25519(hardwarePublicKey.key.value.hexStringToByteArray())

    private val signOneHashTransactionUseCase: SignOneHashTransactionUseCase by inject(
        SignOneHashTransactionUseCase::class.java
    )

    override val key: Bits256
        get() = TODO("No need for hardware wallet")

    override fun publicKey() = pubKey

    override fun sharedKey(publicKey: PublicKeyEd25519): ByteArray {
        TODO("No need for hardware wallet")
    }

    override fun decrypt(data: ByteArray): ByteArray {
        TODO("No need for hardware wallet")
    }

    override fun sign(message: ByteArray): ByteArray {
        return runBlocking {
            val signBytesResponse =
                signOneHashTransactionUseCase(
                    hash = message,
                    walletPublicKey = hardwarePublicKey.publicKey,
                    derivationPath = DerivationPath(hardwarePublicKey.derivationPath)
                )
            when (signBytesResponse) {
                is CompletionResult.Success -> {
                    signBytesResponse.data.signature
                }

                is CompletionResult.Failure -> {
                    throw Exception("Signing failed: ${signBytesResponse.error}")
                }
            }
        }
    }
}