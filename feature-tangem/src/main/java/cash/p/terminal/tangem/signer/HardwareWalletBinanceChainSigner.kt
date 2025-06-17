package cash.p.terminal.tangem.signer

import cash.p.terminal.tangem.common.CustomXPubKeyAddressParser
import cash.p.terminal.tangem.domain.model.AddressBytesWithPublicKey
import cash.p.terminal.tangem.domain.usecase.SignOneHashTransactionUseCase
import cash.p.terminal.wallet.entities.HardwarePublicKey
import com.tangem.common.CompletionResult
import com.tangem.crypto.hdWallet.DerivationPath
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.binancechainkit.core.Wallet
import io.horizontalsystems.binancechainkit.helpers.Crypto
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject

class HardwareWalletBinanceChainSigner(
    private val hardwarePublicKey: HardwarePublicKey,
    private val cardId: String,
    networkType: BinanceChainKit.NetworkType
) : Wallet() {
    private val signOneHashTransactionUseCase: SignOneHashTransactionUseCase by inject(
        SignOneHashTransactionUseCase::class.java
    )
    private val addressBytesWithPublicKey: AddressBytesWithPublicKey =
        CustomXPubKeyAddressParser.parse(hardwarePublicKey.key.value)

    override val address: String =
        Crypto.encodeAddress(networkType.addressPrefix, addressBytesWithPublicKey.addressBytes)
    override var pubKeyForSign: ByteArray = addressBytesWithPublicKey.publicKey

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
