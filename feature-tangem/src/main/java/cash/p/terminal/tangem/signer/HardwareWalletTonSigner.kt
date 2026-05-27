package cash.p.terminal.tangem.signer

import cash.p.terminal.tangem.domain.usecase.SignOneHashTransactionUseCase
import cash.p.terminal.wallet.entities.HardwarePublicKey
import com.tangem.common.CompletionResult
import com.tangem.crypto.hdWallet.DerivationPath
import com.tonapps.blockchain.ton.contract.HashSigner
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import org.ton.bitstring.BitString

class HardwareWalletTonSigner(
    private val hardwarePublicKey: HardwarePublicKey
) : HashSigner {
    private val signOneHashTransactionUseCase: SignOneHashTransactionUseCase by inject(
        SignOneHashTransactionUseCase::class.java
    )

    override fun sign(hash: BitString): BitString {
        val signature = runBlocking {
            val signBytesResponse =
                signOneHashTransactionUseCase(
                    hash = hash.toByteArray(),
                    walletPublicKey = hardwarePublicKey.publicKey,
                    derivationPath = DerivationPath(hardwarePublicKey.derivationPath)
                )
            when (signBytesResponse) {
                is CompletionResult.Success -> {
                    signBytesResponse.data.signature
                }

                is CompletionResult.Failure -> {
                    throw signBytesResponse.error
                }
            }
        }
        return BitString(signature)
    }
}