package cash.p.terminal.tangem.signer

import cash.p.terminal.tangem.domain.usecase.SignOneHashTransactionUseCase
import cash.p.terminal.wallet.entities.HardwarePublicKey
import com.solana.core.Account
import com.solana.core.PublicKey
import com.tangem.common.CompletionResult
import com.tangem.crypto.hdWallet.DerivationPath
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject

class HardwareWalletSolanaAccountSigner(
    override val publicKey: PublicKey,
    private val hardwarePublicKey: HardwarePublicKey
) : Account {
    private val signOneHashTransactionUseCase: SignOneHashTransactionUseCase by inject(
        SignOneHashTransactionUseCase::class.java
    )

    override fun sign(serializedMessage: ByteArray): ByteArray {
        return runBlocking {
            val signBytesResponse =
                signOneHashTransactionUseCase(
                    hash = serializedMessage,
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
